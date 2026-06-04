package com.boms.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    /** 租户编码；空 = 平台超管登录（tenant_id=0）。 */
    private String tenantCode;

    /** 登录账号：用户名或手机号。 */
    @NotBlank
    private String account;

    @NotBlank
    private String password;
}
