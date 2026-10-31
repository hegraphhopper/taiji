package com.taiji.fund.project.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Data
public class ProjectReviewRequest {

    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private BigDecimal score;

    @NotBlank
    private String opinion;

    @NotBlank
    private String result;
}
