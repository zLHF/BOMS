package com.boms.modules.pool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boms.modules.customer.entity.Customer;
import com.boms.modules.customer.mapper.CustomerMapper;
import com.boms.modules.opportunity.entity.Opportunity;
import com.boms.modules.opportunity.entity.OpportunityFollow;
import com.boms.modules.opportunity.mapper.OpportunityFollowMapper;
import com.boms.modules.opportunity.mapper.OpportunityMapper;
import com.boms.modules.pool.entity.PoolConfig;
import com.boms.modules.pool.mapper.PoolConfigMapper;
import com.boms.modules.system.entity.SysUser;
import com.boms.modules.system.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 公海池自动回收定时任务。
 * 每天凌晨 2 点扫描所有租户，将超过配置天数无有效跟进的商机/客户自动回收到公海。
 */
@Slf4j
@Component
public class PoolRecycleScheduler {

    /** 有效跟进类型（决策 D5：电话/拜访/会议/微信/邮件/方案沟通算有效跟进）。 */
    private static final Set<String> VALID_FOLLOW_TYPES = Set.of(
            "电话", "拜访", "会议", "微信", "邮件", "方案", "方案沟通"
    );

    private final PoolConfigMapper configMapper;
    private final OpportunityMapper oppMapper;
    private final CustomerMapper customerMapper;
    private final OpportunityFollowMapper followMapper;
    private final SysUserMapper userMapper;

    public PoolRecycleScheduler(PoolConfigMapper configMapper,
                                OpportunityMapper oppMapper,
                                CustomerMapper customerMapper,
                                OpportunityFollowMapper followMapper,
                                SysUserMapper userMapper) {
        this.configMapper = configMapper;
        this.oppMapper = oppMapper;
        this.customerMapper = customerMapper;
        this.followMapper = followMapper;
        this.userMapper = userMapper;
    }

    @Scheduled(cron = "${boms.pool.cron:0 0 2 * * ?}")
    public void autoRecycle() {
        log.info("[PoolRecycle] 自动回收任务开始");
        List<PoolConfig> configs = configMapper.selectList(null);
        for (PoolConfig cfg : configs) {
            if (cfg.getAutoRecycleEnabled() == null || cfg.getAutoRecycleEnabled() != 1) continue;
            try {
                recycleForTenant(cfg);
            } catch (Exception e) {
                log.error("[PoolRecycle] 租户 {} 回收失败: {}", cfg.getTenantId(), e.getMessage(), e);
            }
        }
        log.info("[PoolRecycle] 自动回收任务结束");
    }

    private void recycleForTenant(PoolConfig cfg) {
        Long tid = cfg.getTenantId();
        LocalDateTime threshold = LocalDateTime.now().minusDays(cfg.getNoFollowDays());
        LocalDateTime protectionThreshold = LocalDateTime.now().minusDays(cfg.getProtectionDays());

        // ---- 商机回收 ----
        LambdaQueryWrapper<Opportunity> oppW = new LambdaQueryWrapper<>();
        oppW.eq(Opportunity::getTenantId, tid);
        oppW.eq(Opportunity::getIsPool, 0);
        oppW.eq(Opportunity::getStatus, "IN_PROGRESS");
        List<Opportunity> opps = oppMapper.selectList(oppW);

        int oppRecycled = 0;
        for (Opportunity o : opps) {
            if (shouldRecycleOpportunity(o, tid, threshold, protectionThreshold, cfg)) {
                doRecycleOpportunity(o, tid, cfg);
                oppRecycled++;
            }
        }

        // ---- 客户回收 ----
        LambdaQueryWrapper<Customer> custW = new LambdaQueryWrapper<>();
        custW.eq(Customer::getTenantId, tid);
        custW.eq(Customer::getIsPool, 0);
        custW.eq(Customer::getStatus, "ACTIVE");
        List<Customer> customers = customerMapper.selectList(custW);

        int custRecycled = 0;
        for (Customer c : customers) {
            if (shouldRecycleCustomer(c, tid, threshold, protectionThreshold, cfg)) {
                doRecycleCustomer(c, tid, cfg);
                custRecycled++;
            }
        }

        log.info("[PoolRecycle] 租户 {}: 回收商机 {}, 回收客户 {}", tid, oppRecycled, custRecycled);
    }

    private boolean shouldRecycleOpportunity(Opportunity o, Long tid, LocalDateTime threshold,
                                              LocalDateTime protectionThreshold, PoolConfig cfg) {
        // 保护期：最近一次跟进在保护期内不回收
        if (o.getLastFollowAt() != null && o.getLastFollowAt().isAfter(protectionThreshold)) {
            return false;
        }

        // 查最近一条有效跟进
        LambdaQueryWrapper<OpportunityFollow> fw = new LambdaQueryWrapper<>();
        fw.eq(OpportunityFollow::getTenantId, tid);
        fw.eq(OpportunityFollow::getOpportunityId, o.getId());
        fw.in(OpportunityFollow::getFollowType, VALID_FOLLOW_TYPES);
        fw.orderByDesc(OpportunityFollow::getCreatedAt);
        fw.last("LIMIT 1");
        OpportunityFollow lastFollow = followMapper.selectOne(fw);

        if (lastFollow == null) {
            // 从未有过有效跟进，用创建时间判断
            return o.getCreatedAt() != null && o.getCreatedAt().isBefore(threshold);
        }
        return lastFollow.getCreatedAt() != null && lastFollow.getCreatedAt().isBefore(threshold);
    }

    private void doRecycleOpportunity(Opportunity o, Long tid, PoolConfig cfg) {
        o.setIsPool(1);
        o.setPoolRecycledAt(LocalDateTime.now());
        o.setPoolReason("系统自动回收：超过" + cfg.getNoFollowDays() + "天无有效跟进");
        o.setOwnerId(null);
        o.setDeptId(null);
        oppMapper.updateById(o);
    }

    private boolean shouldRecycleCustomer(Customer c, Long tid, LocalDateTime threshold,
                                           LocalDateTime protectionThreshold, PoolConfig cfg) {
        // 客户没有直接的跟进记录表（当前跟进在 opportunity_follow 上），
        // 使用 updatedAt 作为简易判断
        if (c.getUpdatedAt() != null && c.getUpdatedAt().isAfter(threshold)) {
            return false;
        }
        // 检查该客户下所有非公海商机是否都有跟进
        LambdaQueryWrapper<Opportunity> oppW = new LambdaQueryWrapper<>();
        oppW.eq(Opportunity::getTenantId, tid);
        oppW.eq(Opportunity::getCustomerId, c.getId());
        oppW.eq(Opportunity::getIsPool, 0);
        oppW.eq(Opportunity::getStatus, "IN_PROGRESS");
        oppW.gt(Opportunity::getLastFollowAt, threshold);
        long activeOppCount = oppMapper.selectCount(oppW);
        // 如果有活跃商机有跟进，则不回收客户
        return activeOppCount == 0;
    }

    private void doRecycleCustomer(Customer c, Long tid, PoolConfig cfg) {
        c.setIsPool(1);
        c.setPoolRecycledAt(LocalDateTime.now());
        c.setPoolReason("系统自动回收：超过" + cfg.getNoFollowDays() + "天无有效跟进");
        c.setOwnerId(null);
        customerMapper.updateById(c);
    }
}
