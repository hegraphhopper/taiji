package com.taiji.fund.support;

import com.taiji.fund.common.api.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仅用于单元测试的受保护探针接口。
 *
 * <p>放在 {@code src/test/java} 内，不会进入生产代码；用于验证
 * {@code @PreAuthorize} 在真实 Security 过滤器链下的 403 行为。</p>
 */
@RestController
@RequestMapping("/api/v1/test/admin-probe")
public class AdminProbeController {

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> probe() {
        return Result.ok("ok");
    }
}
