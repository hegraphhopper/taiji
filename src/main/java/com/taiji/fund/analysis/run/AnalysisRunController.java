package com.taiji.fund.analysis.run;

import com.taiji.fund.analysis.result.dto.AnalysisRunQuery;
import com.taiji.fund.analysis.result.dto.AnalysisRunRequest;
import com.taiji.fund.analysis.result.dto.AnalysisRunVO;
import com.taiji.fund.common.api.PageResult;
import com.taiji.fund.common.api.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 分析运行接口：触发、历史、详情、删除（仅失败运行）。
 */
@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class AnalysisRunController {

    private final AnalysisRunService runService;

    @PostMapping("/runs")
    @PreAuthorize("hasAnyRole('PMO','ADMIN')")
    public Result<AnalysisRunVO> create(@Valid @RequestBody AnalysisRunRequest request) {
        return Result.ok(runService.create(request));
    }

    @GetMapping("/runs")
    @PreAuthorize("hasAnyRole('PMO','ADMIN')")
    public Result<PageResult<AnalysisRunVO>> list(AnalysisRunQuery query) {
        return Result.ok(runService.list(query));
    }

    @GetMapping("/runs/{id}")
    @PreAuthorize("hasAnyRole('PMO','ADMIN')")
    public Result<AnalysisRunVO> detail(@PathVariable Long id) {
        return Result.ok(runService.detail(id));
    }

    @DeleteMapping("/runs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        runService.delete(id);
        return Result.ok();
    }
}
