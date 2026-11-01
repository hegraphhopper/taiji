package com.taiji.fund.analysis.llm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LlmCallLogQueryService {

    private static final long MAX_PAGE_SIZE = 100L;

    private final LlmCallLogMapper callLogMapper;

    @Transactional(readOnly = true)
    public LlmLogPageVO page(LlmCallLogQuery query) {
        LlmCallLogQuery actual = query == null ? new LlmCallLogQuery() : query;
        long pageNo = Math.max(1L, actual.getPage() == null ? 1L : actual.getPage());
        long pageSize = Math.min(MAX_PAGE_SIZE,
                Math.max(1L, actual.getSize() == null ? 20L : actual.getSize()));
        LambdaQueryWrapper<LlmCallLog> wrapper = wrapper(actual);
        Page<LlmCallLog> page = callLogMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        List<LlmCallLogVO> records = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        List<LlmCallLog> all = callLogMapper.selectList(wrapper);
        long success = all.stream().filter(item -> "SUCCESS".equals(item.getStatus())).count();
        long latencySum = all.stream()
                .filter(item -> item.getLatencyMs() != null)
                .mapToLong(LlmCallLog::getLatencyMs).sum();
        long latencyCount = all.stream()
                .filter(item -> item.getLatencyMs() != null).count();
        long promptTokens = all.stream()
                .filter(item -> item.getPromptTokens() != null)
                .mapToLong(LlmCallLog::getPromptTokens).sum();
        long completionTokens = all.stream()
                .filter(item -> item.getCompletionTokens() != null)
                .mapToLong(LlmCallLog::getCompletionTokens).sum();

        return LlmLogPageVO.builder()
                .records(records)
                .total(page.getTotal())
                .page(pageNo)
                .size(pageSize)
                .successCount(success)
                .failureCount(all.size() - success)
                .avgLatencyMs(latencyCount == 0 ? 0 : latencySum / latencyCount)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .build();
    }

    private LambdaQueryWrapper<LlmCallLog> wrapper(LlmCallLogQuery query) {
        LambdaQueryWrapper<LlmCallLog> wrapper = Wrappers.lambdaQuery();
        if (query.getRunId() != null) {
            wrapper.eq(LlmCallLog::getRunId, query.getRunId());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(LlmCallLog::getStatus, query.getStatus().trim().toUpperCase());
        }
        if (StringUtils.hasText(query.getModel())) {
            wrapper.like(LlmCallLog::getModel, query.getModel().trim());
        }
        wrapper.orderByDesc(LlmCallLog::getCreatedAt).orderByDesc(LlmCallLog::getId);
        return wrapper;
    }

    private LlmCallLogVO toVO(LlmCallLog log) {
        return LlmCallLogVO.builder()
                .id(log.getId())
                .runId(log.getRunId())
                .projectId(log.getProjectId())
                .peerProjectId(log.getPeerProjectId())
                .model(log.getModel())
                .status(log.getStatus())
                .latencyMs(log.getLatencyMs())
                .promptTokens(log.getPromptTokens())
                .completionTokens(log.getCompletionTokens())
                .error(log.getError())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
