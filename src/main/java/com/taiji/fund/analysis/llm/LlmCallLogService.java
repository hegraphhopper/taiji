package com.taiji.fund.analysis.llm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * llm_call_log 写入服务。失败只记录 warn，不影响主流程；不记录 prompt 与密钥。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmCallLogService implements LlmCallRecorder {

    private static final int MAX_ERROR_LENGTH = 500;

    private final LlmCallLogMapper callLogMapper;

    @Override
    public void record(LlmCallLogEntry entry) {
        if (entry == null || entry.getStatus() == null) {
            return;
        }
        try {
            LlmCallLog logEntity = new LlmCallLog();
            logEntity.setRunId(entry.getRunId());
            logEntity.setProjectId(entry.getProjectId());
            logEntity.setPeerProjectId(entry.getPeerProjectId());
            logEntity.setModel(entry.getModel());
            logEntity.setStatus(entry.getStatus().name());
            logEntity.setLatencyMs(entry.getLatencyMs());
            logEntity.setPromptTokens(entry.getPromptTokens());
            logEntity.setCompletionTokens(entry.getCompletionTokens());
            logEntity.setError(truncate(entry.getError()));
            callLogMapper.insert(logEntity);
        } catch (Exception exception) {
            log.warn("大模型调用日志写入失败: {}", exception.getMessage());
        }
    }

    private String truncate(String error) {
        if (error == null) {
            return null;
        }
        return error.length() > MAX_ERROR_LENGTH ? error.substring(0, MAX_ERROR_LENGTH) : error;
    }
}
