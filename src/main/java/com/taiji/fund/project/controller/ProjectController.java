package com.taiji.fund.project.controller;

import com.taiji.fund.common.api.PageResult;
import com.taiji.fund.common.api.Result;
import com.taiji.fund.common.enums.ProjectAction;
import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.project.dto.ProjectActionRequest;
import com.taiji.fund.project.dto.ProjectApprovalLogVO;
import com.taiji.fund.project.dto.ProjectDetailVO;
import com.taiji.fund.project.dto.ProjectListItemVO;
import com.taiji.fund.project.dto.ProjectQuery;
import com.taiji.fund.project.dto.ProjectSaveRequest;
import com.taiji.fund.project.dto.SectionSaveRequest;
import com.taiji.fund.project.service.ProjectQueryService;
import com.taiji.fund.project.service.ProjectSectionService;
import com.taiji.fund.project.service.ProjectService;
import com.taiji.fund.project.service.ProjectWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectSectionService sectionService;
    private final ProjectQueryService queryService;
    private final ProjectWorkflowService workflowService;

    @GetMapping
    public Result<PageResult<ProjectListItemVO>> list(@ModelAttribute ProjectQuery query) {
        return Result.ok(queryService.list(query));
    }

    @GetMapping("/{id}")
    public Result<ProjectDetailVO> detail(@PathVariable Long id) {
        return Result.ok(queryService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PMO','GROUP_USER')")
    public Result<ProjectDetailVO> create(@Valid @RequestBody ProjectSaveRequest request) {
        return Result.ok(projectService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PMO','GROUP_USER')")
    public Result<ProjectDetailVO> update(@PathVariable Long id,
                                          @Valid @RequestBody ProjectSaveRequest request) {
        return Result.ok(projectService.update(id, request));
    }

    @PutMapping("/{id}/sections")
    @PreAuthorize("hasAnyRole('ADMIN','PMO','GROUP_USER')")
    public Result<ProjectDetailVO> saveSections(@PathVariable Long id,
                                                @Valid @RequestBody SectionSaveRequest request) {
        sectionService.save(id, request);
        return Result.ok(queryService.detail(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PMO','GROUP_USER')")
    public Result<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/actions")
    @PreAuthorize("hasAnyRole('ADMIN','PMO','GROUP_USER')")
    public Result<ProjectDetailVO> action(@PathVariable Long id,
                                          @Valid @RequestBody ProjectActionRequest request) {
        ProjectAction action;
        try {
            action = ProjectAction.valueOf(request.getAction().trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BizException(ErrorCode.BAD_REQUEST, "不支持的项目操作");
        }
        switch (action) {
            case SUBMIT:
                return Result.ok(workflowService.submit(id, request));
            case WITHDRAW:
                return Result.ok(workflowService.withdraw(id, request));
            case APPROVE:
                return Result.ok(workflowService.approve(id, request));
            case REJECT:
                return Result.ok(workflowService.reject(id, request));
            default:
                throw new BizException(ErrorCode.BAD_REQUEST, "不支持的项目操作");
        }
    }

    @GetMapping("/{id}/logs")
    public Result<List<ProjectApprovalLogVO>> logs(@PathVariable Long id) {
        return Result.ok(workflowService.logs(id));
    }
}
