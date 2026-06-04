package com.boms.modules.pool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boms.common.audit.AuditLog;
import com.boms.common.result.R;
import com.boms.common.security.RequirePerm;
import com.boms.common.tenant.TenantContext;
import com.boms.modules.pool.entity.PoolConfig;
import com.boms.modules.pool.mapper.PoolConfigMapper;
import org.springframework.web.bind.annotation.*;

/** 公海池配置管理。 */
@RestController
@RequestMapping("/api/pool/config")
public class PoolConfigController {

    private final PoolConfigMapper configMapper;
    private final PoolRecycleScheduler scheduler;

    public PoolConfigController(PoolConfigMapper configMapper, PoolRecycleScheduler scheduler) {
        this.configMapper = configMapper;
        this.scheduler = scheduler;
    }

    /** 获取当前租户的公海配置。 */
    @GetMapping
    @RequirePerm("config:pool")
    public R<PoolConfig> get() {
        Long tid = TenantContext.tenantId();
        PoolConfig cfg = configMapper.selectOne(
                new LambdaQueryWrapper<PoolConfig>().eq(PoolConfig::getTenantId, tid));
        if (cfg == null) {
            // 自动创建默认配置
            cfg = new PoolConfig();
            cfg.setTenantId(tid);
            cfg.setAutoRecycleEnabled(1);
            cfg.setNoFollowDays(30);
            cfg.setProtectionDays(7);
            cfg.setPersonalLimit(50);
            configMapper.insert(cfg);
        }
        return R.ok(cfg);
    }

    /** 更新当前租户的公海配置。 */
    @PutMapping
    @RequirePerm("config:pool")
    @AuditLog(action = "config:pool:update", objectType = "pool_config")
    public R<Void> update(@RequestBody PoolConfig body) {
        Long tid = TenantContext.tenantId();
        PoolConfig cfg = configMapper.selectOne(
                new LambdaQueryWrapper<PoolConfig>().eq(PoolConfig::getTenantId, tid));
        if (cfg == null) return R.fail(40400, "配置不存在");

        // 只允许更新安全字段
        if (body.getAutoRecycleEnabled() != null) cfg.setAutoRecycleEnabled(body.getAutoRecycleEnabled());
        if (body.getNoFollowDays() != null) cfg.setNoFollowDays(body.getNoFollowDays());
        if (body.getProtectionDays() != null) cfg.setProtectionDays(body.getProtectionDays());
        if (body.getPersonalLimit() != null) cfg.setPersonalLimit(body.getPersonalLimit());
        configMapper.updateById(cfg);
        return R.ok();
    }

    /** 手动触发一次回收（管理员操作）。 */
    @PostMapping("/execute")
    @RequirePerm("config:pool")
    @AuditLog(action = "config:pool:execute", objectType = "pool_config")
    public R<String> execute() {
        scheduler.autoRecycle();
        return R.ok("回收任务已执行");
    }
}
