package com.taiji.fund.system.dict;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictItemVO {

    private String code;

    private String name;

    private String description;

    public DictItemVO(String code, String name) {
        this(code, name, null);
    }
}
