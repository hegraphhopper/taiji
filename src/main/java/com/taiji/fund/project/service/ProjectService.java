package com.taiji.fund.project.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.taiji.fund.common.audit.Audit;
import com.taiji.fund.common.enums.FundType;
import com.taiji.fund.common.enums.ProjectStatus;
import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.project.domain.ProjectReview;
import com.taiji.fund.project.dto.ProjectDetailVO;
import com.taiji.fund.project.dto.ProjectSaveRequest;
import com.taiji.fund.project.mapper.ProjectMapper;
import com.taiji.fund.project.mapper.ProjectReviewMapper;
import com.taiji.fund.system.group.Group;
import com.taiji.fund.system.group.GroupMapper;
import com.taiji.fund.system.techdirection.TechDirection;
import com.taiji.fund.system.techdirection.TechDirectionMapper;
import com.taiji.fund.system.user.User;
import com.taiji.fund.system.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectMapper projectMapper;
    private final ProjectReviewMapper reviewMapper;
    private final GroupMapper groupMapper;
    private final UserMapper userMapper;
    private final TechDirectionMapper techDirectionMapper;
    private final ProjectCodeGenerator codeGenerator;
    private final ProjectAccessService accessService;
    private final ProjectQueryService queryService;

    @Transactional
    @Audit(action = "PROJECT_CREATE", resourceType = "PROJECT")
    public ProjectDetailVO create(ProjectSaveRequest request) {
        validate(request);
        accessService.assertCanCreate(request.getGroupId());

        String manualCode = normalizeCode(request.getCode());
        if (manualCode != null && codeGenerator.exists(manualCode)) {
            throw new BizException(ErrorCode.PROJECT_CODE_DUPLICATED);
        }

        Project project = new Project();
        applyFields(project, request);
        project.setStatus(ProjectStatus.DRAFT.getCode());
        project.setCreatedBy(accessService.currentUserId());
        project.setDeleted(0);
        project.setVersion(0);

        insertWithCode(project, manualCode);
        return queryService.detail(project.getId());
    }

    @Transactional
    @Audit(action = "PROJECT_UPDATE", resourceType = "PROJECT")
    public ProjectDetailVO update(Long id, ProjectSaveRequest request) {
        Project project = load(id);
        accessService.assertCanEdit(project);
        assertEditable(project);
        validate(request);

        if (!request.getGroupId().equals(project.getGroupId())) {
            accessService.assertCanCreate(request.getGroupId());
        }

        String manualCode = normalizeCode(request.getCode());
        if (manualCode != null && !manualCode.equals(project.getCode()) && codeGenerator.exists(manualCode)) {
            throw new BizException(ErrorCode.PROJECT_CODE_DUPLICATED);
        }
        if (manualCode != null) {
            project.setCode(manualCode);
        }

        applyFields(project, request);
        if (request.getVersion() != null) {
            project.setVersion(request.getVersion());
        }
        int updated = projectMapper.updateById(project);
        if (updated == 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "项目已被他人修改，请刷新后重试");
        }
        return queryService.detail(id);
    }

    @Transactional
    @Audit(action = "PROJECT_DELETE", resourceType = "PROJECT")
    public void delete(Long id) {
        Project project = load(id);
        accessService.assertCanEdit(project);
        if (!ProjectStatus.DRAFT.getCode().equals(project.getStatus())) {
            throw new BizException(ErrorCode.PROJECT_NOT_EDITABLE, "仅草稿项目可删除");
        }
        if (reviewMapper.selectCount(Wrappers.<ProjectReview>lambdaQuery()
                .eq(ProjectReview::getProjectId, id)) > 0) {
            throw new BizException(ErrorCode.PROJECT_NOT_EDITABLE, "存在评审记录的项目不能删除");
        }
        projectMapper.deleteById(id);
    }

    private void validate(ProjectSaveRequest request) {
        if (!FundType.PLATFORM.getCode().equals(request.getFundType())
                && !FundType.INNOVATION.getCode().equals(request.getFundType())
                && !FundType.OTHER.getCode().equals(request.getFundType())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "基金类型不合法");
        }
        Group group = groupMapper.selectById(request.getGroupId());
        if (group == null || Integer.valueOf(0).equals(group.getEnabled())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "项目组不存在或已禁用");
        }
        User owner = userMapper.selectById(request.getOwnerId());
        if (owner == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "负责人不存在");
        }
        if (request.getTechDirectionId() != null) {
            TechDirection direction = techDirectionMapper.selectById(request.getTechDirectionId());
            if (direction == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "技术方向不存在");
            }
        }
        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "结束日期不能早于开始日期");
        }
    }

    private void applyFields(Project project, ProjectSaveRequest request) {
        project.setName(request.getName().trim());
        project.setFundType(request.getFundType());
        project.setGroupId(request.getGroupId());
        project.setOwnerId(request.getOwnerId());
        project.setTechDirectionId(request.getTechDirectionId());
        project.setBudget(request.getBudget());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setSummary(trimToNull(request.getSummary()));
        project.setExpectedOutcome(trimToNull(request.getExpectedOutcome()));
    }

    private void insertWithCode(Project project, String manualCode) {
        if (manualCode != null) {
            project.setCode(manualCode);
            insert(project, true);
            return;
        }
        for (int attempt = 0; attempt < ProjectCodeGenerator.maxAttempts(); attempt++) {
            project.setCode(codeGenerator.nextAvailableCode(attempt));
            if (insert(project, false)) {
                return;
            }
        }
        throw new BizException(ErrorCode.BAD_REQUEST, "项目编码生成冲突，请稍后重试");
    }

    private boolean insert(Project project, boolean manual) {
        try {
            projectMapper.insert(project);
            return true;
        } catch (DuplicateKeyException exception) {
            if (manual) {
                throw new BizException(ErrorCode.PROJECT_CODE_DUPLICATED);
            }
            project.setId(null);
            return false;
        }
    }

    private Project load(Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new BizException(ErrorCode.PROJECT_NOT_FOUND);
        }
        return project;
    }

    private void assertEditable(Project project) {
        if (!ProjectStatus.DRAFT.getCode().equals(project.getStatus())
                && !ProjectStatus.REJECTED.getCode().equals(project.getStatus())) {
            throw new BizException(ErrorCode.PROJECT_NOT_EDITABLE);
        }
    }

    private String normalizeCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
