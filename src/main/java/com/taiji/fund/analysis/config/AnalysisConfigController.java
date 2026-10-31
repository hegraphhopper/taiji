package com.taiji.fund.analysis.config;

import com.taiji.fund.common.api.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analysis/config")
@RequiredArgsConstructor
public class AnalysisConfigController {

    private final AnalysisConfigService configService;

    @GetMapping
    public Result<AnalysisConfigVO> current() {
        return Result.ok(configService.current());
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<AnalysisConfigVO>> history() {
        return Result.ok(configService.history());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<AnalysisConfigVO> save(@Valid @RequestBody AnalysisConfigRequest request) {
        return Result.ok(configService.save(request));
    }
}
