package com.taiji.fund.system.group;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupService {

    private final GroupMapper groupMapper;

    public List<GroupVO> listEnabled() {
        List<Group> groups = groupMapper.selectList(Wrappers.<Group>lambdaQuery()
                .eq(Group::getEnabled, 1)
                .orderByAsc(Group::getSort)
                .orderByAsc(Group::getId));
        return groups.stream().map(this::toVO).collect(Collectors.toList());
    }

    public String findName(Long groupId) {
        if (groupId == null) {
            return null;
        }
        Group group = groupMapper.selectById(groupId);
        return group == null ? null : group.getName();
    }

    private GroupVO toVO(Group group) {
        return GroupVO.builder()
                .id(group.getId())
                .name(group.getName())
                .code(group.getCode())
                .parentId(group.getParentId())
                .leader(group.getLeader())
                .sort(group.getSort())
                .build();
    }
}
