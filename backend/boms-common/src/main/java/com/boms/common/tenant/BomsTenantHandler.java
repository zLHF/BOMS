package com.boms.common.tenant;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

import java.util.Set;

/**
 * MyBatis-Plus 租户行级隔离。
 * - 业务表自动追加 {@code tenant_id = ?}，值取自 {@link TenantContext}。
 * - 平台级表（无 tenant_id）与平台超管（tenant_id=0）跳过追加。
 */
public class BomsTenantHandler implements TenantLineHandler {

    /** 平台级表，本身不带 tenant_id，绝不追加条件。 */
    private static final Set<String> PLATFORM_TABLES = Set.of(
            "tenant", "sys_package", "permission", "flyway_schema_history"
    );

    @Override
    public Expression getTenantId() {
        Long t = TenantContext.tenantId();
        return new LongValue(t == null ? -1L : t);
    }

    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }

    @Override
    public boolean ignoreTable(String tableName) {
        String t = tableName.replace("`", "").toLowerCase();
        if (PLATFORM_TABLES.contains(t)) {
            return true;
        }
        // 无上下文或平台超管：不做租户行级过滤
        return TenantContext.tenantId() == null || TenantContext.isPlatform();
    }
}
