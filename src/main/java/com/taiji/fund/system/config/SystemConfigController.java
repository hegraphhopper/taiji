package com.taiji.fund.system.config;

import com.taiji.fund.common.api.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 系统参数管理：仅管理员。
 */
@RestController
@RequestMapping("/api/v1/system/configs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SystemConfigController {

    private final SystemConfigService configService;

    @GetMapping
    public Result<List<SystemConfigVO>> list() {
        return Result.ok(configService.list());
    }

    @GetMapping("/{key}")
    public Result<SystemConfigVO> get(@PathVariable String key) {
        return Result.ok(configService.get(key));
    }

    @PutMapping("/{key}")
    public Result<SystemConfigVO> update(@PathVariable String key,
                                         @Valid @RequestBody SystemConfigUpdateRequest request) {
        return Result.ok(configService.update(key, request));
    }
}
