package com.taiji.fund.system.techdirection;

import com.taiji.fund.common.api.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tech-directions")
@RequiredArgsConstructor
public class TechDirectionController {

    private final TechDirectionService techDirectionService;

    @GetMapping
    public Result<List<TechDirectionVO>> list() {
        return Result.ok(techDirectionService.listEnabled());
    }
}
