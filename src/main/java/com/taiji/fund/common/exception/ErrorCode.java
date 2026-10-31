package com.taiji.fund.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "成功", 200),

    BAD_REQUEST(40000, "请求参数错误", 400),
    GROUP_HAS_USERS(40011, "项目组下存在用户，无法删除", 400),
    PASSWORD_WEAK(40012, "密码至少 8 位且需同时包含字母和数字", 400),

    UNAUTHORIZED(40100, "未认证或登录已过期", 401),
    AUTH_BAD_CREDENTIALS(40101, "用户名或密码错误", 401),
    AUTH_LOCKED(40102, "账号已锁定，请稍后重试", 401),
    AUTH_TOKEN_INVALID(40103, "登录凭证无效或已过期", 401),
    AUTH_OLD_PASSWORD_INVALID(40104, "原密码不正确", 400),
    AUTH_DISABLED(40105, "账号已被禁用，请联系管理员", 401),

    FORBIDDEN(40301, "没有访问权限", 403),
    NOT_FOUND(40400, "资源不存在", 404),
    METHOD_NOT_ALLOWED(40500, "请求方法不支持", 405),

    UNSUPPORTED_MEDIA_TYPE(41500, "不支持的请求内容类型", 415),

    PROJECT_ILLEGAL_TRANSITION(40010, "项目状态不允许该操作", 400),
    PROJECT_CODE_DUPLICATED(40020, "项目编码已存在", 400),
    PROJECT_NOT_EDITABLE(40021, "当前项目状态不允许修改", 400),
    PROJECT_NOT_FOUND(40410, "项目不存在", 404),
    FILE_TYPE_NOT_ALLOWED(40030, "不支持的文件类型", 400),
    FILE_TOO_LARGE(40031, "文件超过 20MB 限制", 400),
    FILE_LIMIT_EXCEEDED(40032, "单个项目最多 10 个附件", 400),
    FILE_NOT_FOUND(40420, "附件不存在", 404),

    ANALYSIS_CONFIG_INVALID(40040, "分析配置不合法", 400),
    ANALYSIS_WEIGHT_INVALID(40041, "字段权重配置不合法", 400),
    ANALYSIS_THRESHOLD_INVALID(40042, "重复度阈值配置不合法", 400),
    ANALYSIS_TEXT_REQUIRED(40043, "分析文本不能为空", 400),
    ANALYSIS_LLM_CONFIG_INVALID(40044, "大模型配置不合法", 400),

    ANALYSIS_RUN_NOT_FOUND(40430, "分析运行不存在", 404),
    ANALYSIS_PAIR_NOT_FOUND(40431, "分析项目对不存在", 404),
    ANALYSIS_SNAPSHOT_NOT_FOUND(40432, "项目分析快照不存在", 404),

    ANALYSIS_RUN_CONFLICT(40930, "已有分析任务正在运行", 409),

    INTERNAL_ERROR(50000, "服务器内部错误", 500);

    private final int code;
    private final String message;
    private final int httpStatus;

    ErrorCode(int code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
