package com.taiji.fund.insight.investment;

import com.taiji.fund.common.api.Result;
import com.taiji.fund.insight.investment.dto.DistributionItemVO;
import com.taiji.fund.insight.investment.dto.InvestmentOverviewVO;
import com.taiji.fund.insight.investment.dto.InvestmentProjectVO;
import com.taiji.fund.insight.investment.dto.InvestmentQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 技术创新投入态势接口。EXPERT 无权访问。
 */
@RestController
@RequestMapping("/api/v1/insight/investment")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PMO','ADMIN','GROUP_USER')")
public class InvestmentController {

    private final InvestmentQueryService queryService;
    private final InvestmentExcelExporter excelExporter;
    private final InvestmentCsvExporter csvExporter;

    @GetMapping("/overview")
    public Result<InvestmentOverviewVO> overview(InvestmentQuery query) {
        return Result.ok(queryService.overview(query));
    }

    @GetMapping("/by-year")
    public Result<List<DistributionItemVO>> byYear(InvestmentQuery query) {
        return Result.ok(queryService.byYear(query));
    }

    @GetMapping("/by-fund-type")
    public Result<List<DistributionItemVO>> byFundType(InvestmentQuery query) {
        return Result.ok(queryService.byFundType(query));
    }

    @GetMapping("/by-group")
    public Result<List<DistributionItemVO>> byGroup(InvestmentQuery query) {
        return Result.ok(queryService.byGroup(query));
    }

    @GetMapping("/by-direction")
    public Result<List<DistributionItemVO>> byDirection(InvestmentQuery query) {
        return Result.ok(queryService.byDirection(query));
    }

    @GetMapping("/top-risk")
    public Result<List<InvestmentProjectVO>> topRisk(InvestmentQuery query) {
        return Result.ok(queryService.topRisk(query));
    }

    @GetMapping("/projects")
    public Result<List<InvestmentProjectVO>> projects(InvestmentQuery query) {
        return Result.ok(queryService.projects(query));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(InvestmentQuery query) {
        byte[] content = excelExporter.export(query);
        String filename = "investment-" + yearPart(query.getStartYear()) + "_" + yearPart(query.getEndYear()) + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    private String yearPart(Integer year) {
        return year == null ? "all" : String.valueOf(year);
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(InvestmentQuery query) {
        byte[] content = csvExporter.export(query);
        String filename = "investment-" + yearPart(query.getStartYear()) + "_"
                + yearPart(query.getEndYear()) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(content);
    }
}
