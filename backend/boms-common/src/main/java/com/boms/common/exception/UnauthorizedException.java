package com.boms.common.exception;

/** 未登录/Token 失效 → 401。 */
public class UnauthorizedException extends BizException {
    public UnauthorizedException(String message) {
        super(40100, message);
    }
}
