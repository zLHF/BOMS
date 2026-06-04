package com.boms.modules.cpn.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 算力平台通用响应（对外返回给平台时用此格式：code=20000 表示成功）。 */
public record CpnBaseResp(
        int code,
        String msg,
        String traceId,
        Object data
) {
    public static CpnBaseResp ok(Object data) {
        return new CpnBaseResp(20000, "成功", java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16), data);
    }

    public static CpnBaseResp fail(int code, String msg) {
        return new CpnBaseResp(code, msg, java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16), null);
    }
}
