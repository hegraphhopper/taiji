package com.taiji.fund.analysis.run;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.taiji.fund.analysis.config.AnalysisConfigService;
import com.taiji.fund.analysis.config.AnalysisConfigVO;
import com.taiji.fund.analysis.result.AnalysisRun;
import com.taiji.fund.analysis.result.AnalysisRunMapper;
import com.taiji.fund.analysis.result.PeerMatchAssembler;
import com.taiji.fund.analysis.result.RunStatus;
import com.taiji.fund.analysis.result.TriggerType;
import com.taiji.fund.analysis.result.dto.AnalysisPrecheckRequest;
import com.taiji.fund.analysis.result.dto.AnalysisPrecheckVO;
import com.taiji.fund.analysis.result.dto.AnalysisRunQuery;
import com.taiji.fund.analysis.result.dto.AnalysisRunRequest;
import com.taiji.fund.analysis.result.dto.AnalysisRunVO;
import com.taiji.fund.analysis.support.ProjectAnalysisInput;
import com.taiji.fund.analysis.support.ProjectAnalysisInputFactory;
import com.taiji.fund.analysis.tokenizer.TextNormalizer;
import com.taiji.fund.common.api.PageResult;
import com.taiji.fund.common.audit.Audit;
import com.taiji.fund.common.enums.RoleCode;
import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.project.mapper.ProjectMapper;
import com.taiji.fund.project.service.ProjectAccessService;
import com.taiji.fund.security.LoginUser;
import com.taiji.fund.security.SecurityUtils;
import com.taiji.fund.system.techdirection.TechDirection;
import com.taiji.fund.system.techdirection.TechDirectionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 分析运行编排：并发保护、运行记录、失败清理、运行历史与删除。
 *
 * <p>本类不整体开启事务：运行记录先以 RUNNING 提交，计算与落库失败时才能把状态改成 FAILED
 * 并按 run_id 清理明细。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisRunService {

    private static final long MAX_PAGE_SIZE = 100L;
    private static final int MAX_MESSAGE_LENGTH = 500;

    private final AnalysisRunMapper runMapper;
    private final AnalysisExecutor executor;
    private final AnalysisResultWriter resultWriter;
    private final AnalysisConfigService configService;
    private final ProjectMapper projectMapper;
    private final ProjectAnalysisInputFactory inputFactory;
    private final ProjectAccessService accessService;
    private final TechDirectionMapper techDirectionMapper;
    private final PeerMatchAssembler peerMatchAssembler;

    private final AtomicBoolean heavyRunning = new AtomicBoolean(false);
    private final AtomicBoolean precheckRunning = new AtomicBoolean(false);

    @Audit(action = "ANALYSIS_RUN_CREATE", resourceType = "ANALYSIS_RUN")
    public AnalysisRunVO create(AnalysisRunRequest request) {
        TriggerType trigger = TriggerType.parse(request.getTriggerType());
        if (trigger == TriggerType.PRECHECK) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "预检请调用 /api/v1/analysis/precheck");
        }
        if (request.getAnalysisDate() != null && !isAdmin()) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅管理员可指定分析日期");
        }
        Long projectId = request.getProjectId();
        if (trigger == TriggerType.PROJECT && projectId == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "单项目分析必须指定项目");
        }
        if (trigger == TriggerType.FULL) {
            projectId = null;
        }
        if (projectId != null && projectMapper.selectById(projectId) == null) {
            throw new BizException(ErrorCode.PROJECT_NOT_FOUND);
        }

        acquire(heavyRunning);
        try {
            ensureNoRunning();
            AnalysisConfigVO config = configService.current();
            AnalysisRun run = createRunningRun(trigger, projectId, request.getAnalysisDate(), config);
            long start = System.currentTimeMillis();
            try {
                AnalysisBatch batch = executor.execute(trigger, projectId,
                        request.getAnalysisDate(), config, run.getId());
                resultWriter.persist(batch);
                markSuccess(run, batch, start);
            } catch (RuntimeException error) {
                markFailed(run, error, start);
                if (error instanceof BizException) {
                    throw error;
                }
                throw new BizException(ErrorCode.INTERNAL_ERROR, "分析运行失败，请查看运行记录");
            }
            return toVO(run);
        } finally {
            heavyRunning.set(false);
        }
    }

    @Audit(action = "ANALYSIS_PRECHECK", resourceType = "ANALYSIS_RUN")
    public AnalysisPrecheckVO precheck(AnalysisPrecheckRequest request) {
        ProjectAnalysisInput input = buildPrecheckInput(request);
        acquire(precheckRunning);
        try {
            AnalysisConfigVO config = configService.current();
            AnalysisRun run = createRunningRun(TriggerType.PRECHECK, input.getProjectId(), null, config);
            long start = System.currentTimeMillis();
            try {
                PrecheckOutcome outcome = executor.precheck(input, config, run.getId());
                run.setStatus(RunStatus.SUCCESS.name());
                run.setProjectCount(outcome.getProjectCount());
                run.setPairCount(outcome.getPairs().size());
                run.setLlmUsed(outcome.isLlmUsed() ? 1 : 0);
                run.setDurationMs(Math.max(1L, System.currentTimeMillis() - start));
                runMapper.updateById(run);
                return AnalysisPrecheckVO.builder()
                        .runId(run.getId())
                        .projectId(input.getProjectId())
                        .peerCount(outcome.getPairs().size())
                        .configVersion(config.getVersion())
                        .llmUsed(outcome.isLlmUsed())
                        .topPeers(peerMatchAssembler.assemble(
                                outcome.getPairs(), input.getProjectId(), true))
                        .build();
            } catch (RuntimeException error) {
                markFailed(run, error, start);
                if (error instanceof BizException) {
                    throw error;
                }
                throw new BizException(ErrorCode.INTERNAL_ERROR, "预检失败，请稍后重试");
            }
        } finally {
            precheckRunning.set(false);
        }
    }

    @Transactional(readOnly = true)
    public PageResult<AnalysisRunVO> list(AnalysisRunQuery query) {
        LambdaQueryWrapper<AnalysisRun> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getTriggerType())) {
            TriggerType type = TriggerType.parse(query.getTriggerType());
            wrapper.eq(AnalysisRun::getTriggerType, type.name());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(AnalysisRun::getStatus, parseStatus(query.getStatus()));
        }
        if (query.getFrom() != null) {
            wrapper.ge(AnalysisRun::getCreatedAt, query.getFrom().atStartOfDay());
        }
        if (query.getTo() != null) {
            wrapper.le(AnalysisRun::getCreatedAt, query.getTo().atTime(23, 59, 59));
        }
        wrapper.orderByDesc(AnalysisRun::getCreatedAt).orderByDesc(AnalysisRun::getId);

        long pageNo = Math.max(1L, query.getPage());
        long pageSize = Math.min(MAX_PAGE_SIZE, Math.max(1L, query.getSize()));
        Page<AnalysisRun> page = runMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        List<AnalysisRunVO> records = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), pageNo, pageSize);
    }

    @Transactional(readOnly = true)
    public AnalysisRunVO detail(Long id) {
        AnalysisRun run = runMapper.selectById(id);
        if (run == null) {
            throw new BizException(ErrorCode.ANALYSIS_RUN_NOT_FOUND);
        }
        return toVO(run);
    }

    @Audit(action = "ANALYSIS_RUN_DELETE", resourceType = "ANALYSIS_RUN")
    public void delete(Long id) {
        AnalysisRun run = runMapper.selectById(id);
        if (run == null) {
            throw new BizException(ErrorCode.ANALYSIS_RUN_NOT_FOUND);
        }
        if (!RunStatus.FAILED.name().equals(run.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "仅允许删除失败的分析运行");
        }
        resultWriter.deleteRunCascade(id);
    }

    private void ensureNoRunning() {
        Long running = runMapper.selectCount(Wrappers.<AnalysisRun>lambdaQuery()
                .eq(AnalysisRun::getStatus, RunStatus.RUNNING.name())
                .in(AnalysisRun::getTriggerType, TriggerType.FULL.name(), TriggerType.PROJECT.name()));
        if (running != null && running > 0) {
            throw new BizException(ErrorCode.ANALYSIS_RUN_CONFLICT);
        }
    }

    private AnalysisRun createRunningRun(TriggerType trigger, Long projectId, LocalDate analysisDate,
                                         AnalysisConfigVO config) {
        AnalysisRun run = new AnalysisRun();
        run.setTriggerType(trigger.name());
        run.setProjectId(projectId);
        run.setConfigVersion(config.getVersion());
        run.setAnalysisDate(analysisDate == null ? null : analysisDate.atStartOfDay());
        run.setStatus(RunStatus.RUNNING.name());
        run.setProjectCount(0);
        run.setPairCount(0);
        run.setLlmUsed(0);
        run.setOperatorId(SecurityUtils.currentUserId());
        runMapper.insert(run);
        return run;
    }

    private void markSuccess(AnalysisRun run, AnalysisBatch batch, long start) {
        run.setStatus(RunStatus.SUCCESS.name());
        run.setProjectCount(batch.getProjectCount());
        run.setPairCount(batch.getPairCount());
        run.setLlmUsed(batch.isLlmUsed() ? 1 : 0);
        run.setDurationMs(Math.max(1L, System.currentTimeMillis() - start));
        runMapper.updateById(run);
    }

    private void markFailed(AnalysisRun run, RuntimeException error, long start) {
        log.error("分析运行失败: runId={}", run.getId(), error);
        try {
            resultWriter.deleteDetails(run.getId());
        } catch (Exception cleanupError) {
            log.warn("分析运行明细清理失败: runId={}, cause={}", run.getId(),
                    cleanupError.getClass().getSimpleName());
        }
        AnalysisRun update = new AnalysisRun();
        update.setId(run.getId());
        update.setStatus(RunStatus.FAILED.name());
        update.setLlmUsed(0);
        update.setDurationMs(Math.max(1L, System.currentTimeMillis() - start));
        update.setMessage(describe(error));
        runMapper.updateById(update);
    }

    private String describe(RuntimeException error) {
        String message = error.getMessage();
        String value = error.getClass().getSimpleName()
                + (message == null || message.trim().isEmpty() ? "" : ": " + message);
        value = value.replaceAll("[\\r\\n\\t]+", " ").trim();
        return value.length() > MAX_MESSAGE_LENGTH
                ? value.substring(0, MAX_MESSAGE_LENGTH) : value;
    }

    private ProjectAnalysisInput buildPrecheckInput(AnalysisPrecheckRequest request) {
        if (request.getProjectId() != null) {
            Project project = projectMapper.selectById(request.getProjectId());
            if (project == null) {
                throw new BizException(ErrorCode.PROJECT_NOT_FOUND);
            }
            accessService.assertCanView(project);
            return inputFactory.build(request.getProjectId());
        }
        if (!hasText(request.getGoalText()) && !hasText(request.getContentText())
                && !hasText(request.getTechText()) && !hasText(request.getMilestoneText())) {
            throw new BizException(ErrorCode.ANALYSIS_TEXT_REQUIRED);
        }
        Set<String> tags = new LinkedHashSet<>();
        if (request.getTechDirectionId() != null) {
            TechDirection direction = techDirectionMapper.selectById(request.getTechDirectionId());
            if (direction != null && direction.getName() != null) {
                tags.add(direction.getName().trim().toLowerCase(Locale.ROOT));
            }
        }
        return ProjectAnalysisInput.builder()
                .projectId(null)
                .techDirectionId(request.getTechDirectionId())
                .goalText(TextNormalizer.normalize(request.getGoalText()))
                .contentText(TextNormalizer.normalize(request.getContentText()))
                .keyTechText(TextNormalizer.normalize(request.getTechText()))
                .milestoneText(TextNormalizer.normalize(request.getMilestoneText()))
                .tags(tags)
                .build();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean isAdmin() {
        LoginUser user = SecurityUtils.currentUserOrNull();
        return user != null && user.getRoles().contains(RoleCode.ADMIN.name());
    }

    private String parseStatus(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (RunStatus status : RunStatus.values()) {
            if (status.name().equals(normalized)) {
                return normalized;
            }
        }
        throw new BizException(ErrorCode.BAD_REQUEST, "运行状态必须为 RUNNING/SUCCESS/FAILED");
    }

    private void acquire(AtomicBoolean lock) {
        if (!lock.compareAndSet(false, true)) {
            throw new BizException(ErrorCode.ANALYSIS_RUN_CONFLICT);
        }
    }

    private AnalysisRunVO toVO(AnalysisRun run) {
        return AnalysisRunVO.builder()
                .id(run.getId())
                .triggerType(run.getTriggerType())
                .projectId(run.getProjectId())
                .configVersion(run.getConfigVersion())
                .analysisDate(run.getAnalysisDate())
                .status(run.getStatus())
                .projectCount(run.getProjectCount())
                .pairCount(run.getPairCount())
                .llmUsed(Integer.valueOf(1).equals(run.getLlmUsed()))
                .durationMs(run.getDurationMs())
                .message(run.getMessage())
                .operatorId(run.getOperatorId())
                .createdAt(run.getCreatedAt())
                .build();
    }
}
