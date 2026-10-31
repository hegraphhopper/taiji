package com.taiji.fund.analysis.result.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.time.LocalDate;

/**
 * 触发分析运行请求。
 */
@Data
public class AnalysisRunRequest {

    @NotBlank(message = "触发类型不能为空")
    private String triggerType;

    private Long projectId;

    /**
     * 历史回填日期，仅 ADMIN 可用；为空表示按当前时间分析。
     */
    private LocalDate analysisDate;
}
