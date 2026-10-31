package com.taiji.fund.analysis.result;

import com.taiji.fund.common.exception.BizException;
import com.taiji.fund.common.exception.ErrorCode;

/**
 * 分析运行触发类型：全量、单项目、预检。
 */
public enum TriggerType {

    FULL,
    PROJECT,
    PRECHECK;

    /**
     * 解析前端提交的触发类型，非法值统一返回 400，避免落入 500。
     */
    public static TriggerType parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "触发类型不能为空");
        }
        String normalized = value.trim().toUpperCase(java.util.Locale.ROOT);
        for (TriggerType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        throw new BizException(ErrorCode.BAD_REQUEST, "触发类型必须为 FULL/PROJECT/PRECHECK");
    }
}
