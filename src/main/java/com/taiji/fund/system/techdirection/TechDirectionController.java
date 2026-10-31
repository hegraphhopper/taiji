package com.taiji.fund.system.techdirection;

import com.taiji.fund.common.api.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
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

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<TechDirectionVO>> listAll() {
        return Result.ok(techDirectionService.listAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<TechDirectionVO> create(@Valid @RequestBody TechDirectionSaveRequest request) {
        return Result.ok(techDirectionService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<TechDirectionVO> update(@PathVariable Long id,
                                          @Valid @RequestBody TechDirectionSaveRequest request) {
        return Result.ok(techDirectionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        techDirectionService.delete(id);
        return Result.ok();
    }
}
