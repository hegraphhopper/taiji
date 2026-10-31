package com.taiji.fund.system.config;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class SystemConfigUpdateRequest {

    @NotBlank
    private String value;

    private String description;
}
