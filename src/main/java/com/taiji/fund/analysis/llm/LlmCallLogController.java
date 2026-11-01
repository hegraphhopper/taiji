package com.taiji.fund.analysis.llm;

import com.taiji.fund.common.api.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analysis/llm-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','PMO')")
public class LlmCallLogController {

    private final LlmCallLogQueryService queryService;

    @GetMapping
    public Result<LlmLogPageVO> page(LlmCallLogQuery query) {
        return Result.ok(queryService.page(query));
    }
}
