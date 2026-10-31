package com.taiji.fund.project.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProjectSaveRequest {

    @NotBlank(message = "项目名称不能为空")
    @Size(max = 200, message = "项目名称不能超过 200 字")
    private String name;

    @Size(max = 50, message = "项目编码不能超过 50 字")
    private String code;

    @NotBlank(message = "基金类型不能为空")
    private String fundType;

    @NotNull(message = "项目组不能为空")
    private Long groupId;

    @NotNull(message = "负责人不能为空")
    private Long ownerId;

    private Long techDirectionId;

    @NotNull(message = "预算不能为空")
    @DecimalMin(value = "0.01", message = "预算必须大于 0")
    @Digits(integer = 12, fraction = 2, message = "预算最多 12 位整数、2 位小数")
    private BigDecimal budget;

    private LocalDate startDate;

    private LocalDate endDate;

    @Size(max = 2000, message = "摘要不能超过 2000 字")
    private String summary;

    @Size(max = 2000, message = "预期成果不能超过 2000 字")
    private String expectedOutcome;

    private Integer version;
}
