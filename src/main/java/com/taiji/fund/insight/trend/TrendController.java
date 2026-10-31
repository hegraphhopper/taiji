package com.taiji.fund.insight.trend;

import com.taiji.fund.common.api.Result;
import com.taiji.fund.insight.trend.dto.AttributionResponseVO;
import com.taiji.fund.insight.trend.dto.DrillProjectVO;
import com.taiji.fund.insight.trend.dto.HeatmapResponseVO;
import com.taiji.fund.insight.trend.dto.TrendDrillQuery;
import com.taiji.fund.insight.trend.dto.TrendQuery;
import com.taiji.fund.insight.trend.dto.TrendResponseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

/**
 * 趋势与归因看板接口。EXPERT 无权访问（403）。
 */
@RestController
@RequestMapping("/api/v1/insight")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PMO','ADMIN','GROUP_USER')")
public class TrendController {

    private final TrendQueryService trendQueryService;
    private final AttributionService attributionService;
    private final HeatmapService heatmapService;
    private final TrendCsvExporter csvExporter;

    @GetMapping("/trend")
    public Result<TrendResponseVO> trend(TrendQuery query) {
        return Result.ok(trendQueryService.trend(query));
    }

    @GetMapping("/attribution")
    public Result<AttributionResponseVO> attribution(TrendQuery query) {
        return Result.ok(attributionService.attribution(query));
    }

    @GetMapping("/heatmap")
    public Result<HeatmapResponseVO> heatmap(TrendQuery query,
                                             @RequestParam(defaultValue = "GROUP_MONTH") String dimension,
                                             @RequestParam(defaultValue = "avg") String metric) {
        return Result.ok(heatmapService.heatmap(query, dimension, metric));
    }

    @GetMapping("/trend/projects")
    public Result<List<DrillProjectVO>> drill(TrendDrillQuery query) {
        return Result.ok(trendQueryService.drill(query));
    }

    @GetMapping("/trend/export")
    public ResponseEntity<byte[]> export(TrendQuery query) {
        TrendResponseVO response = trendQueryService.exportSource(query);
        byte[] content = csvExporter.export(response);
        String filename = "trend-" + response.getStartMonth() + "_" + response.getEndMonth() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encode(filename))
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(content);
    }

    private String encode(String filename) {
        try {
            return URLEncoder.encode(filename, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException exception) {
            return "trend.csv";
        }
    }
}
