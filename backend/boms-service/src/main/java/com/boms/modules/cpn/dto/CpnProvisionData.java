package com.boms.modules.cpn.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 用户开通响应 data 部分。 */
@Data
@AllArgsConstructor
public class CpnProvisionData {
    private String account;
    private String token;
    private String tenantId;
}
