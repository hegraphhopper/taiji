package com.taiji.fund.system.group;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.taiji.fund.common.audit.Audit;
import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.project.mapper.ProjectMapper;
import com.taiji.fund.system.user.User;
import com.taiji.fund.system.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupService {

    private final GroupMapper groupMapper;
    private final UserMapper userMapper;
    private final ProjectMapper projectMapper;

    public List<GroupVO> listEnabled() {
        return list(true);
    }

    public List<GroupVO> listAll() {
        return list(false);
    }

    public String findName(Long groupId) {
        if (groupId == null) {
            return null;
        }
        Group group = groupMapper.selectById(groupId);
        return group == null ? null : group.getName();
    }

    @Audit(action = "SYSTEM_GROUP_CREATE", resourceType = "SYS_GROUP")
    @Transactional
    public GroupVO create(GroupSaveRequest request) {
        validate(request, null);
        Group group = new Group();
        group.setName(request.getName().trim());
        group.setCode(request.getCode().trim());
        group.setParentId(request.getParentId());
        group.setLeader(trim(request.getLeader()));
        group.setSort(request.getSort() == null ? 0 : request.getSort());
        group.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
        group.setDeleted(0);
        groupMapper.insert(group);
        return toVO(group);
    }

    @Audit(action = "SYSTEM_GROUP_UPDATE", resourceType = "SYS_GROUP")
    @Transactional
    public GroupVO update(Long id, GroupSaveRequest request) {
        Group group = require(id);
        validate(request, id);
        checkParent(id, request.getParentId());
        group.setName(request.getName().trim());
        group.setCode(request.getCode().trim());
        group.setParentId(request.getParentId());
        group.setLeader(trim(request.getLeader()));
        group.setSort(request.getSort() == null ? group.getSort() : request.getSort());
        group.setEnabled(request.getEnabled() == null ? group.getEnabled() : request.getEnabled());
        groupMapper.updateById(group);
        return toVO(group);
    }

    @Audit(action = "SYSTEM_GROUP_DELETE", resourceType = "SYS_GROUP")
    @Transactional
    public void delete(Long id) {
        require(id);
        Long users = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getGroupId, id));
        if (users != null && users > 0) {
            throw new BizException(ErrorCode.GROUP_HAS_USERS);
        }
        Long projects = projectMapper.selectCount(Wrappers.<Project>lambdaQuery()
                .eq(Project::getGroupId, id));
        if (projects != null && projects > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "项目组下存在项目，无法删除");
        }
        Long children = groupMapper.selectCount(Wrappers.<Group>lambdaQuery()
                .eq(Group::getParentId, id));
        if (children != null && children > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "存在下级项目组，无法删除");
        }
        groupMapper.deleteById(id);
    }

    private List<GroupVO> list(boolean enabledOnly) {
        List<Group> groups = groupMapper.selectList(Wrappers.<Group>lambdaQuery()
                .eq(enabledOnly, Group::getEnabled, 1)
                .orderByAsc(Group::getSort)
                .orderByAsc(Group::getId));
        return groups.stream().map(this::toVO).collect(Collectors.toList());
    }

    private void validate(GroupSaveRequest request, Long excludeId) {
        if (request == null || !StringUtils.hasText(request.getName())
                || !StringUtils.hasText(request.getCode())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "项目组名称与编码不能为空");
        }
        Group duplicate = groupMapper.selectOne(Wrappers.<Group>lambdaQuery()
                .eq(Group::getCode, request.getCode().trim())
                .ne(excludeId != null, Group::getId, excludeId));
        if (duplicate != null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "项目组编码已存在");
        }
        if (request.getParentId() != null && groupMapper.selectById(request.getParentId()) == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "上级项目组不存在");
        }
    }

    private void checkParent(Long id, Long parentId) {
        Long current = parentId;
        int guard = 0;
        while (current != null && guard++ < 20) {
            if (id.equals(current)) {
                throw new BizException(ErrorCode.BAD_REQUEST, "上级项目组不能是自身或下级");
            }
            Group parent = groupMapper.selectById(current);
            current = parent == null ? null : parent.getParentId();
        }
    }

    private Group require(Long id) {
        Group group = groupMapper.selectById(id);
        if (group == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "项目组不存在");
        }
        return group;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private GroupVO toVO(Group group) {
        return GroupVO.builder()
                .id(group.getId())
                .name(group.getName())
                .code(group.getCode())
                .parentId(group.getParentId())
                .leader(group.getLeader())
                .sort(group.getSort())
                .enabled(group.getEnabled())
                .build();
    }
}
