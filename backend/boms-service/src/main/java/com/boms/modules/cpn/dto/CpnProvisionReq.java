package com.boms.modules.cpn.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** 算力平台用户开通请求（平台调用 BOMS 时发送）。 */
@Data
public class CpnProvisionReq {
    private String enterpriseName;
    private String pltUserCn;
    private String pltAccountLogin;
    private Integer purchaseDuration;
    private String purchaseUnit;
    private String appKey;
    private String appSecret;
    private String code;
    private String pltEmail;
    private String pltMobile;
}
