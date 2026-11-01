package com.taiji.fund.dashboard;

import com.taiji.fund.common.api.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工作台汇总接口：登录用户按各自数据范围查看。
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardQueryService queryService;

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public Result<DashboardSummaryVO> summary() {
        return Result.ok(queryService.summary());
    }
}
