package com.boms.modules.cpn;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 算力平台对接配置。 */
@Data
@Configuration
@ConfigurationProperties(prefix = "boms.cpn")
public class CpnConfig {
    private boolean enabled = false;
    private String gateway;
    private String appKey;
    private String appSecret;
    private String heartbeatCron = "0 */5 * * * ?";
}
