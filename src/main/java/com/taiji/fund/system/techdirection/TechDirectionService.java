package com.taiji.fund.system.techdirection;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.taiji.fund.common.audit.Audit;
import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;
import com.taiji.fund.project.domain.Project;
import com.taiji.fund.project.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TechDirectionService {

    private final TechDirectionMapper techDirectionMapper;
    private final ProjectMapper projectMapper;

    public List<TechDirectionVO> listEnabled() {
        return list(true);
    }

    public List<TechDirectionVO> listAll() {
        return list(false);
    }

    @Audit(action = "SYSTEM_DICT_CREATE", resourceType = "TECH_DIRECTION")
    @Transactional
    public TechDirectionVO create(TechDirectionSaveRequest request) {
        validate(request, null);
        TechDirection direction = new TechDirection();
        apply(direction, request, true);
        direction.setDeleted(0);
        techDirectionMapper.insert(direction);
        return toVO(direction);
    }

    @Audit(action = "SYSTEM_DICT_UPDATE", resourceType = "TECH_DIRECTION")
    @Transactional
    public TechDirectionVO update(Long id, TechDirectionSaveRequest request) {
        TechDirection direction = require(id);
        validate(request, id);
        apply(direction, request, false);
        techDirectionMapper.updateById(direction);
        return toVO(direction);
    }

    @Audit(action = "SYSTEM_DICT_DELETE", resourceType = "TECH_DIRECTION")
    @Transactional
    public void delete(Long id) {
        require(id);
        Long projects = projectMapper.selectCount(Wrappers.<Project>lambdaQuery()
                .eq(Project::getTechDirectionId, id));
        if (projects != null && projects > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "技术方向已被项目引用，无法删除");
        }
        Long children = techDirectionMapper.selectCount(Wrappers.<TechDirection>lambdaQuery()
                .eq(TechDirection::getParentId, id));
        if (children != null && children > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "存在下级技术方向，无法删除");
        }
        techDirectionMapper.deleteById(id);
    }

    private List<TechDirectionVO> list(boolean enabledOnly) {
        List<TechDirection> directions = techDirectionMapper.selectList(
                Wrappers.<TechDirection>lambdaQuery()
                        .eq(enabledOnly, TechDirection::getEnabled, 1)
                        .orderByAsc(TechDirection::getSort)
                        .orderByAsc(TechDirection::getId));
        return directions.stream().map(this::toVO).collect(Collectors.toList());
    }

    private void validate(TechDirectionSaveRequest request, Long excludeId) {
        if (request == null || !StringUtils.hasText(request.getName())
                || !StringUtils.hasText(request.getCode())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "技术方向名称与编码不能为空");
        }
        TechDirection duplicate = techDirectionMapper.selectOne(Wrappers.<TechDirection>lambdaQuery()
                .eq(TechDirection::getCode, request.getCode().trim())
                .ne(excludeId != null, TechDirection::getId, excludeId));
        if (duplicate != null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "技术方向编码已存在");
        }
        if (request.getParentId() != null
                && techDirectionMapper.selectById(request.getParentId()) == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "上级技术方向不存在");
        }
        if (excludeId != null) {
            Long parentId = request.getParentId();
            int guard = 0;
            while (parentId != null && guard++ < 20) {
                if (excludeId.equals(parentId)) {
                    throw new BizException(ErrorCode.BAD_REQUEST, "上级技术方向不能是自身或下级");
                }
                TechDirection parent = techDirectionMapper.selectById(parentId);
                parentId = parent == null ? null : parent.getParentId();
            }
        }
    }

    private void apply(TechDirection direction, TechDirectionSaveRequest request, boolean create) {
        direction.setName(request.getName().trim());
        direction.setCode(request.getCode().trim());
        direction.setParentId(request.getParentId());
        direction.setKeywords(joinKeywords(request.getKeywords()));
        direction.setSort(request.getSort() == null
                ? (create ? 0 : direction.getSort()) : request.getSort());
        direction.setEnabled(request.getEnabled() == null
                ? (create ? 1 : direction.getEnabled()) : request.getEnabled());
    }

    private TechDirection require(Long id) {
        TechDirection direction = techDirectionMapper.selectById(id);
        if (direction == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "技术方向不存在");
        }
        return direction;
    }

    private TechDirectionVO toVO(TechDirection direction) {
        return TechDirectionVO.builder()
                .id(direction.getId())
                .code(direction.getCode())
                .name(direction.getName())
                .parentId(direction.getParentId())
                .keywords(splitKeywords(direction.getKeywords()))
                .sort(direction.getSort())
                .enabled(direction.getEnabled())
                .build();
    }

    private String joinKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return "";
        }
        return keywords.stream()
                .filter(keyword -> keyword != null && !keyword.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining(","));
    }

    private List<String> splitKeywords(String keywords) {
        if (keywords == null || keywords.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(keywords.split(","))
                .map(String::trim)
                .filter(keyword -> !keyword.isEmpty())
                .collect(Collectors.toList());
    }
}
