package com.taiji.fund.analysis.result;

import com.taiji.fund.analysis.result.dto.AnalysisPrecheckRequest;
import com.taiji.fund.analysis.result.dto.AnalysisPrecheckVO;
import com.taiji.fund.analysis.result.dto.PairDetailVO;
import com.taiji.fund.analysis.result.dto.ProjectAnalysisReportVO;
import com.taiji.fund.analysis.run.AnalysisRunService;
import com.taiji.fund.common.api.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 分析查询接口：项目报告、项目对明细、预检。
 */
@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class AnalysisQueryController {

    private final AnalysisQueryService queryService;
    private final AnalysisRunService runService;

    @GetMapping("/projects/{id}")
    public Result<ProjectAnalysisReportVO> report(@PathVariable Long id) {
        return Result.ok(queryService.report(id));
    }

    @GetMapping("/pairs/{id}")
    public Result<PairDetailVO> pairDetail(@PathVariable Long id) {
        return Result.ok(queryService.pairDetail(id));
    }

    @PostMapping("/precheck")
    @PreAuthorize("hasAnyRole('GROUP_USER','PMO','ADMIN')")
    public Result<AnalysisPrecheckVO> precheck(@Valid @RequestBody AnalysisPrecheckRequest request) {
        return Result.ok(runService.precheck(request));
    }
}
