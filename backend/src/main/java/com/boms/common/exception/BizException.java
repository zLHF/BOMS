package com.boms.common.exception;

import lombok.Getter;

/** 业务异常，携带错误码。 */
@Getter
public class BizException extends RuntimeException {
    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message) {
        this(40000, message);
    }
}
