package com.boms.modules.cpn;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 算力平台心跳定时任务（每 5 分钟上报一次）。 */
@Slf4j
@Component
@ConditionalOnProperty(name = "boms.cpn.enabled", havingValue = "true")
public class CpnHeartbeatScheduler {

    private final CpnClient cpnClient;

    public CpnHeartbeatScheduler(CpnClient cpnClient) {
        this.cpnClient = cpnClient;
    }

    @Scheduled(cron = "${boms.cpn.heartbeat-cron:0 */5 * * * ?}")
    public void heartbeat() {
        try {
            boolean ok = cpnClient.heartbeat();
            if (!ok) {
                log.warn("[CPN-Heartbeat] 心跳上报失败");
            }
        } catch (Exception e) {
            log.error("[CPN-Heartbeat] 心跳异常: {}", e.getMessage());
        }
    }
}
