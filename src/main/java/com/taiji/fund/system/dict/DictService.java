package com.taiji.fund.system.dict;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.taiji.fund.common.enums.FundType;
import com.taiji.fund.common.enums.ProjectStatus;
import com.taiji.fund.system.role.Role;
import com.taiji.fund.system.role.RoleMapper;
import com.taiji.fund.system.techdirection.TechDirectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DictService {

    private final RoleMapper roleMapper;
    private final TechDirectionService techDirectionService;

    public DictVO load() {
        List<DictItemVO> fundTypes = Arrays.stream(FundType.values())
                .map(type -> new DictItemVO(type.getCode(), type.getLabel()))
                .collect(Collectors.toList());
        List<DictItemVO> projectStatuses = Arrays.stream(ProjectStatus.values())
                .map(status -> new DictItemVO(status.getCode(), status.getLabel()))
                .collect(Collectors.toList());
        List<DictItemVO> roles = roleMapper.selectList(Wrappers.<Role>lambdaQuery().orderByAsc(Role::getId))
                .stream()
                .map(role -> new DictItemVO(role.getCode(), role.getName(), role.getDescription()))
                .collect(Collectors.toList());
        return DictVO.builder()
                .fundTypes(fundTypes)
                .projectStatuses(projectStatuses)
                .roles(roles)
                .techDirections(techDirectionService.listEnabled())
                .build();
    }
}
