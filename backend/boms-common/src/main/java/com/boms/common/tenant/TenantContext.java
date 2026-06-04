package com.boms.common.tenant;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 租户/用户上下文（ThreadLocal）。
 * 由 JwtAuthFilter 在请求入口解析 token 并加载权限后写入，请求结束清理。
 * tenant_id=0 表示平台超管（逻辑平台，无 tenant 表记录）。
 */
public final class TenantContext {

    /**
     * @param tenantId    租户ID（0=平台）
     * @param userId      用户ID
     * @param username    登录名
     * @param deptId      主部门（数据范围过滤用）
     * @param permissions 权限码集合
     * @param dataScope   有效数据范围（多角色取最宽）
     * @param roleCodes   角色码列表
     */
    public record Principal(
            Long tenantId,
            Long userId,
            String username,
            Long deptId,
            Set<String> permissions,
            String dataScope,
            List<String> roleCodes
    ) {
        public Principal(Long tenantId, Long userId, String username) {
            this(tenantId, userId, username, null, Collections.emptySet(), "SELF", Collections.emptyList());
        }
    }

    private static final ThreadLocal<Principal> HOLDER = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(Principal principal) {
        HOLDER.set(principal);
    }

    public static Principal get() {
        return HOLDER.get();
    }

    public static Long tenantId() {
        Principal p = HOLDER.get();
        return p == null ? null : p.tenantId();
    }

    public static Long userId() {
        Principal p = HOLDER.get();
        return p == null ? null : p.userId();
    }

    public static boolean isPlatform() {
        Long t = tenantId();
        return t != null && t == 0L;
    }

    public static boolean hasPerm(String code) {
        Principal p = HOLDER.get();
        return p != null && p.permissions().contains(code);
    }

    public static void clear() {
        HOLDER.remove();
    }
}
