package com.taiji.fund.system.dict;

import com.taiji.fund.system.techdirection.TechDirectionVO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DictVO {

    private List<DictItemVO> fundTypes;

    private List<DictItemVO> projectStatuses;

    private List<DictItemVO> roles;

    private List<TechDirectionVO> techDirections;
}
