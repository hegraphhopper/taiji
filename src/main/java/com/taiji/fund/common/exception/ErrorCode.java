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
