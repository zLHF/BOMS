package com.boms.common.security;

import com.boms.common.tenant.TenantContext;

/**
 * 加载完整用户上下文（权限码/角色/数据范围/部门）。
 * 接口在 common，实现在 modules.system，避免 common 反向依赖业务模块。
 */
public interface PrincipalLoader {
    TenantContext.Principal load(Long userId, Long tenantId, String username);
}
