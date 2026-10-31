package com.taiji.fund.system.group;

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
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @GetMapping
    public Result<List<GroupVO>> list() {
        return Result.ok(groupService.listEnabled());
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<GroupVO>> listAll() {
        return Result.ok(groupService.listAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<GroupVO> create(@Valid @RequestBody GroupSaveRequest request) {
        return Result.ok(groupService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<GroupVO> update(@PathVariable Long id,
                                  @Valid @RequestBody GroupSaveRequest request) {
        return Result.ok(groupService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        groupService.delete(id);
        return Result.ok();
    }
}
