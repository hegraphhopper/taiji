package com.taiji.fund.system.techdirection;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TechDirectionService {

    private final TechDirectionMapper techDirectionMapper;

    public List<TechDirectionVO> listEnabled() {
        List<TechDirection> directions = techDirectionMapper.selectList(Wrappers.<TechDirection>lambdaQuery()
                .eq(TechDirection::getEnabled, 1)
                .orderByAsc(TechDirection::getSort)
                .orderByAsc(TechDirection::getId));
        return directions.stream().map(this::toVO).collect(Collectors.toList());
    }

    private TechDirectionVO toVO(TechDirection direction) {
        return TechDirectionVO.builder()
                .id(direction.getId())
                .code(direction.getCode())
                .name(direction.getName())
                .parentId(direction.getParentId())
                .keywords(splitKeywords(direction.getKeywords()))
                .sort(direction.getSort())
                .build();
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
