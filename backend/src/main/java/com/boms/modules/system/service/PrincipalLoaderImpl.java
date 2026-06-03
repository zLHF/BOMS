package com.boms.modules.system.service;

import com.boms.common.scope.DataScope;
import com.boms.common.security.PrincipalLoader;
import com.boms.common.tenant.TenantContext;
import com.boms.modules.system.entity.Role;
import com.boms.modules.system.entity.SysUser;
import com.boms.modules.system.mapper.PermissionMapper;
import com.boms.modules.system.mapper.RoleMapper;
import com.boms.modules.system.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/** 从 DB 装配完整用户上下文（权限码/角色/数据范围/部门）。 */
@Service
public class PrincipalLoaderImpl implements PrincipalLoader {

    private final PermissionMapper permissionMapper;
    private final RoleMapper roleMapper;
    private final SysUserMapper userMapper;

    public PrincipalLoaderImpl(PermissionMapper permissionMapper, RoleMapper roleMapper, SysUserMapper userMapper) {
        this.permissionMapper = permissionMapper;
        this.roleMapper = roleMapper;
        this.userMapper = userMapper;
    }

    @Override
    public TenantContext.Principal load(Long userId, Long tenantId, String username) {
        Set<String> perms = Set.copyOf(permissionMapper.selectCodesByUserId(userId));
        List<Role> roles = roleMapper.selectByUserId(userId);

        DataScope scope = DataScope.SELF;
        for (Role r : roles) {
            scope = DataScope.widest(scope, DataScope.of(r.getDataScope()));
        }
        List<String> roleCodes = roles.stream().map(Role::getCode).toList();

        SysUser u = userMapper.selectById(userId);
        Long deptId = u == null ? null : u.getDeptId();

        return new TenantContext.Principal(tenantId, userId, username, deptId, perms, scope.name(), roleCodes);
    }
}
