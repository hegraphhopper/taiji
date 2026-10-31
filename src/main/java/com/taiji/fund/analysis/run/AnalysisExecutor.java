package com.taiji.fund.analysis.run;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.taiji.fund.analysis.config.AnalysisConfigVO;
import com.taiji.fund.analysis.config.FieldWeights;
import com.taiji.fund.analysis.config.Thresholds;
import com.taiji.fund.analysis.llm.LlmCache;
import com.taiji.fund.analysis.llm.LlmCallRequest;
import com.taiji.fund.analysis.llm.LlmCallResult;
import com.taiji.fund.analysis.llm.LlmCircuitBreaker;
import com.taiji.fund.analysis.llm.LlmClient;
import com.taiji.fund.analysis.llm.LlmOptions;
import com.taiji.fund.analysis.llm.LlmPromptBuilder;
import com.taiji.fund.analysis.llm.LlmProperties;
import com.taiji.fund.analysis.result.AnalysisPair;
import com.taiji.fund.analysis.result.Direction;
import com.taiji.fund.analysis.result.DuplicateSnapshot;
import com.taiji.fund.analysis.result.Relation;
import com.taiji.fund.analysis.result.TriggerType;
import com.taiji.fund.analysis.score.Attribution;
import com.taiji.fund.analysis.score.AttributionResolver;
import com.taiji.fund.analysis.score.DimensionScorer;
import com.taiji.fund.analysis.score.DimensionScores;
import com.taiji.fund.analysis.score.DuplicateLevel;
import com.taiji.fund.analysis.score.MatchPoint;
import com.taiji.fund.analysis.score.MatchSnippetExtractor;
import com.taiji.fund.analysis.score.ScoreFusion;
import com.taiji.fund.analysis.support.ProjectAnalysisInput;
import com.taiji.fund.analysis.support.ProjectAnalysisInputFactory;
import com.taiji.fund.analysis.support.TechTermDictionaryProvider;
import com.taiji.fund.analysis.tokenizer.ChineseTokenizer;
import com.taiji.fund.analysis.tokenizer.TermDictionary;
import com.taiji.fund.analysis.tokenizer.WeightedToken;
import com.taiji.fund.analysis.vector.SparseVector;
import com.taiji.fund.analysis.vector.TfIdfVectorizer;
import com.taiji.fund.common.enums.ProjectStatus;
import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.project.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 重复度分析执行器：构建语料、计算项目对、Top-K 截断、可选大模型融合、快照聚合。
 *
 * <p>计算阶段不开启数据库事务；落库由 {@link AnalysisResultWriter} 在同一事务内完成，
 * 失败时由运行服务按 run_id 清理，避免留下半成品。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisExecutor {

    private static final int SNIPPET_TOP_N = 5;
    private static final int DEFAULT_TOP_K = 5;
    private static final int DEFAULT_LOCAL_WEIGHT = 70;
    private static final int DEFAULT_LLM_WEIGHT = 30;
    private static final int DEFAULT_LLM_TOP_N = 5;
    private static final int DEFAULT_TIMEOUT_MS = 8000;
    private static final int DEFAULT_SIMULTANEOUS_DAYS = 30;
    private static final int SCORE_SCALE = 2;

    private final ProjectAnalysisInputFactory inputFactory;
    private final ProjectMapper projectMapper;
    private final TechTermDictionaryProvider dictionaryProvider;
    private final LlmClient llmClient;
    private final LlmPromptBuilder promptBuilder;
    private final LlmProperties llmProperties;

    private final TfIdfVectorizer vectorizer = new TfIdfVectorizer();
    private final DimensionScorer dimensionScorer = new DimensionScorer();
    private final ScoreFusion scoreFusion = new ScoreFusion();
    private final AttributionResolver attributionResolver = new AttributionResolver();
    private final MatchSnippetExtractor snippetExtractor = new MatchSnippetExtractor();

    /**
     * 全量 / 单项目运行。
     */
    public AnalysisBatch execute(TriggerType trigger, Long projectId, LocalDate analysisDate,
                                 AnalysisConfigVO config, Long runId) {
        List<Project> corpus = loadCorpus(analysisDate);
        Project target = null;
        if (trigger == TriggerType.PROJECT) {
            if (projectId == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "单项目分析必须指定项目");
            }
            target = projectMapper.selectById(projectId);
            if (target == null) {
                throw new BizException(ErrorCode.PROJECT_NOT_FOUND);
            }
            if (!containsProject(corpus, projectId)) {
                corpus = new ArrayList<>(corpus);
                corpus.add(target);
            }
        }
        if (corpus.isEmpty()) {
            return AnalysisBatch.empty();
        }

        List<ProjectAnalysisInput> inputs = new ArrayList<>(inputFactory.buildAll(corpus));
        inputs.sort(Comparator.comparing(ProjectAnalysisInput::getProjectId,
                Comparator.nullsLast(Comparator.naturalOrder())));

        CorpusVectors vectors = vectorize(inputs);
        Thresholds thresholds = resolveThresholds(config);
        FieldWeights weights = FieldWeights.of(config.getFieldWeights());
        int topK = positive(config.getTopK(), DEFAULT_TOP_K);
        int targetIndex = target == null ? -1 : indexOf(inputs, target.getId());
        if (trigger == TriggerType.PROJECT && targetIndex < 0) {
            throw new BizException(ErrorCode.PROJECT_NOT_FOUND);
        }

        List<PairDraft> drafts = new ArrayList<>();
        for (int left = 0; left < inputs.size(); left++) {
            for (int right = left + 1; right < inputs.size(); right++) {
                if (targetIndex >= 0 && left != targetIndex && right != targetIndex) {
                    continue;
                }
                PairDraft draft = scorePair(inputs, vectors, weights, thresholds, left, right, config);
                if (draft.localScore >= thresholds.getLow()) {
                    drafts.add(draft);
                }
            }
        }

        List<PairDraft> selected = selectTopKUnion(drafts, topK);
        boolean llmUsed = applyLlm(selected, inputs, config, runId);

        int localWeight = config.getLocalWeight() == null ? DEFAULT_LOCAL_WEIGHT : config.getLocalWeight();
        int llmWeight = config.getLlmWeight() == null ? DEFAULT_LLM_WEIGHT : config.getLlmWeight();
        List<PairDraft> persisted = new ArrayList<>(selected.size());
        for (PairDraft draft : selected) {
            draft.totalScore = scoreFusion.fuse(draft.localScore, draft.llmScore, localWeight, llmWeight);
            draft.level = DuplicateLevel.of(draft.totalScore, thresholds);
            if (draft.totalScore >= thresholds.getLow()) {
                persisted.add(draft);
            }
        }
        persisted.sort(Comparator.comparingInt((PairDraft draft) -> draft.left)
                .thenComparingInt(draft -> draft.right));

        List<AnalysisPair> pairs = new ArrayList<>(persisted.size());
        for (PairDraft draft : persisted) {
            pairs.add(toEntity(draft, inputs, vectors, runId));
        }

        Set<Long> snapshotScope = new LinkedHashSet<>();
        if (targetIndex >= 0) {
            snapshotScope.add(inputs.get(targetIndex).getProjectId());
        } else {
            for (ProjectAnalysisInput input : inputs) {
                snapshotScope.add(input.getProjectId());
            }
        }

        LocalDate effectiveDate = analysisDate == null ? LocalDate.now() : analysisDate;
        Map<Long, Project> projectById = indexProjects(corpus);
        List<DuplicateSnapshot> snapshots = new ArrayList<>();
        List<ProjectScoreUpdate> updates = new ArrayList<>();
        for (ProjectAnalysisInput input : inputs) {
            if (!snapshotScope.contains(input.getProjectId())) {
                continue;
            }
            Project project = projectById.get(input.getProjectId());
            snapshots.add(buildSnapshot(input, project, persisted, topK, thresholds, effectiveDate, runId));
            double max = maxScore(persisted, input.getProjectId());
            updates.add(new ProjectScoreUpdate(input.getProjectId(), scale(max),
                    DuplicateLevel.of(max, thresholds).name()));
        }

        return AnalysisBatch.builder()
                .projectCount(inputs.size())
                .pairCount(pairs.size())
                .llmUsed(llmUsed)
                // 历史回填（analysisDate 非空）只沉淀快照，不回写项目当前分数，避免旧月份覆盖最新结论
                .writeProjectScores(analysisDate == null)
                .pairs(pairs)
                .snapshots(snapshots)
                .projectScores(updates)
                .build();
    }

    /**
     * 预检：临时输入（或草稿项目）× 非草稿语料，返回 Top-K，不落项目对与快照。
     */
    public PrecheckOutcome precheck(ProjectAnalysisInput target, AnalysisConfigVO config, Long runId) {
        if (target == null) {
            throw new BizException(ErrorCode.ANALYSIS_TEXT_REQUIRED);
        }
        List<Project> corpus = loadCorpus(null);
        List<ProjectAnalysisInput> peers = new ArrayList<>(inputFactory.buildAll(corpus));
        peers.removeIf(input -> input.getProjectId() != null
                && input.getProjectId().equals(target.getProjectId()));

        List<ProjectAnalysisInput> inputs = new ArrayList<>(peers);
        int targetIndex = inputs.size();
        inputs.add(target);
        if (inputs.size() < 2) {
            return new PrecheckOutcome(Collections.emptyList(), inputs.size());
        }

        CorpusVectors vectors = vectorize(inputs);
        Thresholds thresholds = resolveThresholds(config);
        FieldWeights weights = FieldWeights.of(config.getFieldWeights());

        List<AnalysisPair> results = new ArrayList<>(peers.size());
        for (int index = 0; index < peers.size(); index++) {
            PairDraft draft = scorePair(inputs, vectors, weights, thresholds, targetIndex, index, config);
            draft.llmScore = null;
            draft.totalScore = draft.localScore;
            draft.level = DuplicateLevel.of(draft.totalScore, thresholds);
            results.add(toEntity(draft, inputs, vectors, runId));
        }
        results.sort(Comparator.comparingDouble(
                        (AnalysisPair pair) -> pair.getTotalScore().doubleValue()).reversed()
                .thenComparing(AnalysisPair::getPeerProjectId,
                        Comparator.nullsLast(Comparator.naturalOrder())));
        int topK = positive(config.getTopK(), DEFAULT_TOP_K);
        List<AnalysisPair> topPeers = results.size() <= topK
                ? results : new ArrayList<>(results.subList(0, topK));
        return new PrecheckOutcome(topPeers, inputs.size());
    }

    private PairDraft scorePair(List<ProjectAnalysisInput> inputs, CorpusVectors vectors,
                                FieldWeights weights, Thresholds thresholds,
                                int left, int right, AnalysisConfigVO config) {
        ProjectAnalysisInput first = inputs.get(left);
        ProjectAnalysisInput second = inputs.get(right);
        DimensionScores scores = dimensionScorer.score(first,
                vectors.goal.get(left), vectors.content.get(left), vectors.tech.get(left),
                second,
                vectors.goal.get(right), vectors.content.get(right), vectors.tech.get(right));
        double localScore = allMissing(scores) ? 0D : scoreFusion.fuse(scores, weights);
        PairDraft draft = new PairDraft();
        draft.left = left;
        draft.right = right;
        draft.leftProjectId = first.getProjectId();
        draft.rightProjectId = second.getProjectId();
        draft.scores = scores;
        draft.localScore = localScore;
        draft.totalScore = localScore;
        draft.level = DuplicateLevel.of(localScore, thresholds);
        int simultaneousDays = config.getSimultaneousDays() == null
                ? DEFAULT_SIMULTANEOUS_DAYS : config.getSimultaneousDays();
        draft.attribution = attributionResolver.resolve(first, second, simultaneousDays);
        return draft;
    }

    /**
     * 落库集合 = 任一项目自己 Top-K 的并集；排序以本地分为准，保证大模型结果不影响入选范围。
     */
    private List<PairDraft> selectTopKUnion(List<PairDraft> drafts, int topK) {
        if (drafts.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<PairDraft>> byProject = new LinkedHashMap<>();
        for (PairDraft draft : drafts) {
            byProject.computeIfAbsent(draft.leftProjectId, key -> new ArrayList<>()).add(draft);
            byProject.computeIfAbsent(draft.rightProjectId, key -> new ArrayList<>()).add(draft);
        }
        Set<String> keys = new LinkedHashSet<>();
        for (List<PairDraft> projectDrafts : byProject.values()) {
            projectDrafts.sort(Comparator.comparingDouble((PairDraft draft) -> draft.localScore).reversed()
                    .thenComparingInt(draft -> draft.left)
                    .thenComparingInt(draft -> draft.right));
            int limit = Math.min(topK, projectDrafts.size());
            for (int index = 0; index < limit; index++) {
                keys.add(projectDrafts.get(index).key());
            }
        }
        List<PairDraft> selected = new ArrayList<>(keys.size());
        for (PairDraft draft : drafts) {
            if (keys.contains(draft.key())) {
                selected.add(draft);
            }
        }
        return selected;
    }

    private boolean applyLlm(List<PairDraft> selected, List<ProjectAnalysisInput> inputs,
                             AnalysisConfigVO config, Long runId) {
        if (selected.isEmpty()) {
            return false;
        }
        int timeout = config.getLlmTimeoutMs() == null ? DEFAULT_TIMEOUT_MS : config.getLlmTimeoutMs();
        LlmOptions options = llmProperties.toOptions(Boolean.TRUE.equals(config.getLlmEnabled()), timeout);
        if (!options.usable()) {
            return false;
        }
        int topN = positive(config.getLlmTopN(), DEFAULT_LLM_TOP_N);
        List<PairDraft> candidates = new ArrayList<>(selected);
        candidates.sort(Comparator.comparingDouble((PairDraft draft) -> draft.localScore).reversed()
                .thenComparingInt(draft -> draft.left)
                .thenComparingInt(draft -> draft.right));
        if (candidates.size() > topN) {
            candidates = new ArrayList<>(candidates.subList(0, topN));
        }

        LlmCircuitBreaker breaker = new LlmCircuitBreaker();
        boolean llmUsed = false;
        int configVersion = config.getVersion() == null ? 0 : config.getVersion();
        for (PairDraft draft : candidates) {
            ProjectAnalysisInput first = inputs.get(draft.left);
            ProjectAnalysisInput second = inputs.get(draft.right);
            String prompt = promptBuilder.userPrompt(first, second);
            String textHash = LlmCache.hashText(first.getGoalText(), first.getContentText(),
                    first.getKeyTechText(), second.getGoalText(), second.getContentText(),
                    second.getKeyTechText());
            LlmCallResult result = llmClient.call(LlmCallRequest.builder()
                    .runId(runId)
                    .projectId(first.getProjectId())
                    .peerId(second.getProjectId())
                    .textHash(textHash)
                    .configVersion(configVersion)
                    .userPrompt(prompt)
                    .build(), options, breaker);
            if (result.isSuccess() && result.getResponse() != null) {
                draft.llmScore = result.getResponse().getSemanticScore();
                draft.llmReason = result.getResponse().getReason();
                llmUsed = true;
            }
            if (breaker.isOpen()) {
                log.warn("大模型连续失败达到阈值，本次运行停止调用: runId={}", runId);
                break;
            }
        }
        return llmUsed;
    }

    private AnalysisPair toEntity(PairDraft draft, List<ProjectAnalysisInput> inputs,
                                  CorpusVectors vectors, Long runId) {
        ProjectAnalysisInput first = inputs.get(draft.left);
        ProjectAnalysisInput second = inputs.get(draft.right);
        AnalysisPair pair = new AnalysisPair();
        pair.setRunId(runId);
        pair.setProjectId(first.getProjectId());
        pair.setPeerProjectId(second.getProjectId());
        pair.setGoalScore(scale(draft.scores.getGoalScore()));
        pair.setContentScore(scale(draft.scores.getContentScore()));
        pair.setTechScore(scale(draft.scores.getTechScore()));
        pair.setMilestoneScore(scale(draft.scores.getMilestoneScore()));
        pair.setTagScore(scale(draft.scores.getTagScore()));
        pair.setLocalScore(scale(draft.localScore));
        pair.setLlmScore(draft.llmScore == null ? null : scale(draft.llmScore));
        pair.setTotalScore(scale(draft.totalScore));
        pair.setLevel(draft.level.name());
        pair.setRelation(draft.attribution.getRelation());
        pair.setDirection(draft.attribution.getDirection());
        pair.setEarlierProjectId(draft.attribution.getEarlierProjectId());
        pair.setMatchedPoints(snippetExtractor.toJson(collectPoints(first, second,
                vectors.goal.get(draft.left), vectors.goal.get(draft.right),
                vectors.content.get(draft.left), vectors.content.get(draft.right),
                vectors.tech.get(draft.left), vectors.tech.get(draft.right), true)));
        pair.setDiffPoints(snippetExtractor.toJson(collectPoints(first, second,
                vectors.goal.get(draft.left), vectors.goal.get(draft.right),
                vectors.content.get(draft.left), vectors.content.get(draft.right),
                vectors.tech.get(draft.left), vectors.tech.get(draft.right), false)));
        pair.setLlmReason(draft.llmReason);
        return pair;
    }

    /**
     * 逐维度提取片段：Round 1 提取器按单向量工作，此处按维度构造输入视图，避免跨维度复用词表。
     */
    private List<MatchPoint> collectPoints(ProjectAnalysisInput first, ProjectAnalysisInput second,
                                           SparseVector firstGoal, SparseVector secondGoal,
                                           SparseVector firstContent, SparseVector secondContent,
                                           SparseVector firstTech, SparseVector secondTech,
                                           boolean matched) {
        List<MatchPoint> points = new ArrayList<>();
        points.addAll(dimensionPoints(first, second, FieldWeights.GOAL, firstGoal, secondGoal, matched));
        points.addAll(dimensionPoints(first, second, FieldWeights.CONTENT,
                firstContent, secondContent, matched));
        points.addAll(dimensionPoints(first, second, FieldWeights.TECH, firstTech, secondTech, matched));
        return points;
    }

    private List<MatchPoint> dimensionPoints(ProjectAnalysisInput first, ProjectAnalysisInput second,
                                             String dimension, SparseVector firstVector,
                                             SparseVector secondVector, boolean matched) {
        ProjectAnalysisInput view = ProjectAnalysisInput.builder()
                .projectId(first.getProjectId())
                .goalText(FieldWeights.GOAL.equals(dimension) ? first.getGoalText() : null)
                .contentText(FieldWeights.CONTENT.equals(dimension) ? first.getContentText() : null)
                .keyTechText(FieldWeights.TECH.equals(dimension) ? first.getKeyTechText() : null)
                .build();
        if (matched) {
            return snippetExtractor.matchedPoints(view, firstVector, secondVector, SNIPPET_TOP_N);
        }
        return snippetExtractor.diffPoints(view, firstVector, secondVector, SNIPPET_TOP_N);
    }

    private DuplicateSnapshot buildSnapshot(ProjectAnalysisInput input, Project project,
                                            List<PairDraft> persisted, int topK,
                                            Thresholds thresholds, LocalDate analysisDate,
                                            Long runId) {
        List<PairDraft> involving = new ArrayList<>();
        for (PairDraft draft : persisted) {
            if (input.getProjectId().equals(draft.leftProjectId)
                    || input.getProjectId().equals(draft.rightProjectId)) {
                involving.add(draft);
            }
        }
        involving.sort(Comparator.comparingDouble((PairDraft draft) -> draft.totalScore).reversed()
                .thenComparingInt(draft -> draft.left)
                .thenComparingInt(draft -> draft.right));

        double max = involving.isEmpty() ? 0D : involving.get(0).totalScore;
        int limit = Math.min(topK, involving.size());
        double average = 0D;
        if (limit > 0) {
            double sum = 0D;
            for (int position = 0; position < limit; position++) {
                sum += involving.get(position).totalScore;
            }
            average = sum / limit;
        }

        int highCount = 0;
        int crossCount = 0;
        int selfCount = 0;
        int ourEarlier = 0;
        int peerEarlier = 0;
        int simultaneous = 0;
        for (PairDraft draft : involving) {
            if (DuplicateLevel.HIGH.name().equals(draft.level.name())) {
                highCount++;
            }
            if (Relation.SELF_GROUP.name().equals(draft.attribution.getRelation())) {
                selfCount++;
                continue;
            }
            crossCount++;
            if (Direction.SIMULTANEOUS.name().equals(draft.attribution.getDirection())) {
                simultaneous++;
            } else if (input.getProjectId().equals(draft.attribution.getEarlierProjectId())) {
                ourEarlier++;
            } else {
                peerEarlier++;
            }
        }

        DuplicateSnapshot snapshot = new DuplicateSnapshot();
        snapshot.setRunId(runId);
        snapshot.setProjectId(input.getProjectId());
        snapshot.setGroupId(input.getGroupId());
        snapshot.setAnalysisDate(analysisDate);
        snapshot.setMaxScore(scale(max));
        snapshot.setAvgTopScore(scale(average));
        snapshot.setHighPairCount(highCount);
        snapshot.setCrossGroupCount(crossCount);
        snapshot.setSelfGroupCount(selfCount);
        snapshot.setOurEarlierCount(ourEarlier);
        snapshot.setPeerEarlierCount(peerEarlier);
        snapshot.setSimultaneousCount(simultaneous);
        if (project != null && project.getBudget() != null) {
            snapshot.setRiskExposure(project.getBudget()
                    .multiply(BigDecimal.valueOf(max))
                    .divide(BigDecimal.valueOf(100D), SCORE_SCALE, RoundingMode.HALF_UP));
        }
        return snapshot;
    }

    private double maxScore(List<PairDraft> persisted, Long projectId) {
        double max = 0D;
        for (PairDraft draft : persisted) {
            if (projectId.equals(draft.leftProjectId) || projectId.equals(draft.rightProjectId)) {
                max = Math.max(max, draft.totalScore);
            }
        }
        return max;
    }

    private CorpusVectors vectorize(List<ProjectAnalysisInput> inputs) {
        TermDictionary dictionary = dictionaryProvider.get();
        List<List<WeightedToken>> goals = new ArrayList<>(inputs.size());
        List<List<WeightedToken>> contents = new ArrayList<>(inputs.size());
        List<List<WeightedToken>> techs = new ArrayList<>(inputs.size());
        for (ProjectAnalysisInput input : inputs) {
            goals.add(ChineseTokenizer.tokenize(input.getGoalText(), dictionary));
            contents.add(ChineseTokenizer.tokenize(input.getContentText(), dictionary));
            techs.add(ChineseTokenizer.tokenize(input.getKeyTechText(), dictionary));
        }
        return new CorpusVectors(vectorizer.vectorize(goals), vectorizer.vectorize(contents),
                vectorizer.vectorize(techs));
    }

    private List<Project> loadCorpus(LocalDate analysisDate) {
        List<Project> projects = projectMapper.selectList(Wrappers.<Project>lambdaQuery()
                .ne(Project::getStatus, ProjectStatus.DRAFT.getCode())
                .orderByAsc(Project::getId));
        if (analysisDate == null) {
            return projects;
        }
        LocalDateTime limit = analysisDate.atTime(23, 59, 59);
        List<Project> filtered = new ArrayList<>(projects.size());
        for (Project project : projects) {
            LocalDateTime time = project.getSubmitTime() == null
                    ? project.getCreatedAt() : project.getSubmitTime();
            if (time == null || !time.isAfter(limit)) {
                filtered.add(project);
            }
        }
        return filtered;
    }

    private Map<Long, Project> indexProjects(List<Project> projects) {
        Map<Long, Project> map = new HashMap<>(projects.size() * 2);
        for (Project project : projects) {
            map.put(project.getId(), project);
        }
        return map;
    }

    private boolean containsProject(List<Project> projects, Long projectId) {
        for (Project project : projects) {
            if (projectId.equals(project.getId())) {
                return true;
            }
        }
        return false;
    }

    private int indexOf(List<ProjectAnalysisInput> inputs, Long projectId) {
        for (int index = 0; index < inputs.size(); index++) {
            if (projectId != null && projectId.equals(inputs.get(index).getProjectId())) {
                return index;
            }
        }
        return -1;
    }

    private Thresholds resolveThresholds(AnalysisConfigVO config) {
        Map<String, Integer> map = config.getThresholds();
        if (!Thresholds.containsAllKeys(map)) {
            return Thresholds.defaults();
        }
        Thresholds thresholds = Thresholds.of(map);
        return thresholds.isValid() ? thresholds : Thresholds.defaults();
    }

    private int positive(Integer value, int fallback) {
        return value == null || value <= 0 ? fallback : value;
    }

    private boolean allMissing(DimensionScores scores) {
        return scores.isMissing(FieldWeights.GOAL)
                && scores.isMissing(FieldWeights.CONTENT)
                && scores.isMissing(FieldWeights.TECH)
                && scores.isMissing(FieldWeights.MILESTONE)
                && scores.isMissing(FieldWeights.TAG);
    }

    private BigDecimal scale(double value) {
        double bounded = Math.max(0D, Math.min(100D, value));
        return BigDecimal.valueOf(bounded).setScale(SCORE_SCALE, RoundingMode.HALF_UP);
    }

    private static final class CorpusVectors {
        private final List<SparseVector> goal;
        private final List<SparseVector> content;
        private final List<SparseVector> tech;

        private CorpusVectors(List<SparseVector> goal, List<SparseVector> content,
                              List<SparseVector> tech) {
            this.goal = goal;
            this.content = content;
            this.tech = tech;
        }
    }

    private static final class PairDraft {
        private int left;
        private int right;
        private Long leftProjectId;
        private Long rightProjectId;
        private DimensionScores scores;
        private double localScore;
        private Double llmScore;
        private double totalScore;
        private DuplicateLevel level;
        private Attribution attribution;
        private String llmReason;

        private String key() {
            return left + ":" + right;
        }
    }
}
