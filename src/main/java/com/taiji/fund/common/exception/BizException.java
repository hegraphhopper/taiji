package com.taiji.fund.common.exception;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;
    private final int httpStatus;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.httpStatus = errorCode.getHttpStatus();
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message == null || message.isEmpty() ? errorCode.getMessage() : message);
        this.code = errorCode.getCode();
        this.httpStatus = errorCode.getHttpStatus();
    }
}
