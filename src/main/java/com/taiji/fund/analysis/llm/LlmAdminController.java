package com.taiji.fund.analysis.llm;

import com.taiji.fund.common.api.Result;
import com.taiji.fund.common.audit.Audit;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 大模型管理接口：仅管理员可见。
 */
@RestController
@RequestMapping("/api/v1/system/llm")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class LlmAdminController {

    private final LlmConnectionTestService connectionTestService;

    @PostMapping("/test")
    @Audit(action = "LLM_CONNECTION_TEST", resourceType = "LLM_CONFIG")
    public Result<LlmTestVO> test() {
        return Result.ok(connectionTestService.test());
    }
}
