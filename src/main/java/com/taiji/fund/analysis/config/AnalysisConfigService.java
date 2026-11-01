package com.taiji.fund.analysis.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taiji.fund.common.audit.Audit;
import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.security.LoginUser;
import com.taiji.fund.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分析配置：单条生效配置 + 版本自增历史。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisConfigService {

    private static final int DEFAULT_TOP_K = 5;
    private static final int DEFAULT_SIMULTANEOUS_DAYS = 30;
    private static final int DEFAULT_LOCAL_WEIGHT = 100;
    private static final int DEFAULT_LLM_WEIGHT = 0;
    private static final int DEFAULT_LLM_TOP_N = 5;
    private static final int DEFAULT_LLM_TIMEOUT_MS = 8000;

    private static final TypeReference<LinkedHashMap<String, Integer>> INT_MAP_TYPE =
            new TypeReference<LinkedHashMap<String, Integer>>() {
            };

    private final AnalysisConfigMapper configMapper;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public AnalysisConfigVO current() {
        AnalysisConfig config = findActive();
        if (config == null) {
            throw new BizException(ErrorCode.ANALYSIS_CONFIG_INVALID, "未找到生效的分析配置");
        }
        return toVO(config);
    }

    @Transactional(readOnly = true)
    public List<AnalysisConfigVO> history() {
        return configMapper.selectList(Wrappers.<AnalysisConfig>lambdaQuery()
                        .orderByDesc(AnalysisConfig::getVersion))
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Transactional
    @Audit(action = "ANALYSIS_CONFIG_UPDATE", resourceType = "ANALYSIS_CONFIG")
    public AnalysisConfigVO save(AnalysisConfigRequest request) {
        FieldWeights weights = validateWeights(request.getFieldWeights());
        Thresholds thresholds = validateThresholds(request.getThresholds());
        int topK = validateRange(request.getTopK(), DEFAULT_TOP_K, 1, 20, "Top-K");
        int simultaneousDays = validateRange(request.getSimultaneousDays(), DEFAULT_SIMULTANEOUS_DAYS,
                0, 365, "同期窗口天数");
        int localWeight = validateMin(request.getLocalWeight(), DEFAULT_LOCAL_WEIGHT, "本地权重");
        int llmWeight = validateMin(request.getLlmWeight(), DEFAULT_LLM_WEIGHT, "大模型权重");
        if (localWeight + llmWeight <= 0) {
            throw new BizException(ErrorCode.ANALYSIS_WEIGHT_INVALID, "本地权重与大模型权重之和必须大于 0");
        }
        int llmTopN = validateRange(request.getLlmTopN(), DEFAULT_LLM_TOP_N, 1, 20, "大模型候选对数");
        int llmTimeoutMs = validateRange(request.getLlmTimeoutMs(), DEFAULT_LLM_TIMEOUT_MS,
                500, 60000, "大模型超时时间");

        AnalysisConfig config = new AnalysisConfig();
        config.setVersion(nextVersion());
        config.setFieldWeights(writeJson(weights.toMap()));
        config.setThresholds(writeJson(thresholds.toMap()));
        config.setTopK(topK);
        config.setSimultaneousDays(simultaneousDays);
        config.setLlmEnabled(Boolean.TRUE.equals(request.getLlmEnabled()) ? 1 : 0);
        config.setLocalWeight(localWeight);
        config.setLlmWeight(llmWeight);
        config.setLlmTopN(llmTopN);
        config.setLlmTimeoutMs(llmTimeoutMs);
        config.setActive(1);
        LoginUser currentUser = SecurityUtils.currentUserOrNull();
        config.setCreatedBy(currentUser == null ? null : currentUser.getId());

        configMapper.update(null, Wrappers.<AnalysisConfig>lambdaUpdate()
                .eq(AnalysisConfig::getActive, 1)
                .set(AnalysisConfig::getActive, 0));
        configMapper.insert(config);
        return toVO(config);
    }

    private AnalysisConfig findActive() {
        List<AnalysisConfig> activeConfigs = configMapper.selectList(Wrappers.<AnalysisConfig>lambdaQuery()
                .eq(AnalysisConfig::getActive, 1)
                .orderByDesc(AnalysisConfig::getVersion));
        return activeConfigs.isEmpty() ? null : activeConfigs.get(0);
    }

    private int nextVersion() {
        List<AnalysisConfig> configs = configMapper.selectList(Wrappers.<AnalysisConfig>lambdaQuery()
                .orderByDesc(AnalysisConfig::getVersion));
        if (configs.isEmpty()) {
            return 1;
        }
        Integer version = configs.get(0).getVersion();
        return version == null ? 1 : version + 1;
    }

    private FieldWeights validateWeights(Map<String, Integer> map) {
        if (!FieldWeights.containsAllKeys(map)) {
            throw new BizException(ErrorCode.ANALYSIS_WEIGHT_INVALID,
                    "字段权重必须包含 goal/content/tech/milestone/tag");
        }
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            Integer value = entry.getValue();
            if (value == null || value < 0) {
                throw new BizException(ErrorCode.ANALYSIS_WEIGHT_INVALID, "字段权重不能为负数");
            }
        }
        FieldWeights weights = FieldWeights.of(map);
        if (weights.total() <= 0) {
            throw new BizException(ErrorCode.ANALYSIS_WEIGHT_INVALID, "字段权重之和必须大于 0");
        }
        return weights;
    }

    private Thresholds validateThresholds(Map<String, Integer> map) {
        if (!Thresholds.containsAllKeys(map)) {
            throw new BizException(ErrorCode.ANALYSIS_THRESHOLD_INVALID, "阈值必须包含 low/medium/high");
        }
        Thresholds thresholds = Thresholds.of(map);
        if (!thresholds.isValid()) {
            throw new BizException(ErrorCode.ANALYSIS_THRESHOLD_INVALID,
                    "阈值必须满足 0 < low < medium < high <= 100");
        }
        return thresholds;
    }

    private int validateRange(Integer value, int defaultValue, int min, int max, String field) {
        int actual = value == null ? defaultValue : value;
        if (actual < min || actual > max) {
            throw new BizException(ErrorCode.ANALYSIS_CONFIG_INVALID,
                    field + " 必须在 " + min + " 到 " + max + " 之间");
        }
        return actual;
    }

    private int validateMin(Integer value, int defaultValue, String field) {
        int actual = value == null ? defaultValue : value;
        if (actual < 0) {
            throw new BizException(ErrorCode.ANALYSIS_WEIGHT_INVALID, field + " 不能为负数");
        }
        return actual;
    }

    private String writeJson(Map<String, Integer> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException exception) {
            log.error("分析配置序列化失败", exception);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "分析配置序列化失败");
        }
    }

    private AnalysisConfigVO toVO(AnalysisConfig config) {
        return AnalysisConfigVO.builder()
                .id(config.getId())
                .version(config.getVersion())
                .fieldWeights(parseJson(config.getFieldWeights(),
                        FieldWeights.defaults().toMap(), "field_weights", config.getVersion()))
                .thresholds(parseJson(config.getThresholds(),
                        Thresholds.defaults().toMap(), "thresholds", config.getVersion()))
                .topK(config.getTopK())
                .simultaneousDays(config.getSimultaneousDays())
                .llmEnabled(Integer.valueOf(1).equals(config.getLlmEnabled()))
                .localWeight(config.getLocalWeight())
                .llmWeight(config.getLlmWeight())
                .llmTopN(config.getLlmTopN())
                .llmTimeoutMs(config.getLlmTimeoutMs())
                .active(Integer.valueOf(1).equals(config.getActive()))
                .createdBy(config.getCreatedBy())
                .createdAt(config.getCreatedAt())
                .build();
    }

    private Map<String, Integer> parseJson(String json, Map<String, Integer> fallback,
                                           String field, Integer version) {
        if (json == null || json.trim().isEmpty()) {
            return fallback;
        }
        try {
            LinkedHashMap<String, Integer> parsed = objectMapper.readValue(json, INT_MAP_TYPE);
            return Collections.unmodifiableMap(parsed);
        } catch (JsonProcessingException exception) {
            log.warn("分析配置 JSON 解析失败，使用默认值: field={}, version={}", field, version);
            return fallback;
        }
    }
}
