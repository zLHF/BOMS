package com.boms.common.exception;

/** 无权限/越权 → 403。 */
public class ForbiddenException extends BizException {
    public ForbiddenException(String message) {
        super(40300, message);
    }
}
