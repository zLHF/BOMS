package com.boms.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boms.common.exception.BizException;
import com.boms.modules.system.dto.AssignPermReq;
import com.boms.modules.system.dto.RoleReq;
import com.boms.modules.system.entity.Permission;
import com.boms.modules.system.entity.Role;
import com.boms.modules.system.entity.RolePermission;
import com.boms.modules.system.mapper.PermissionMapper;
import com.boms.modules.system.mapper.RoleMapper;
import com.boms.modules.system.mapper.RolePermissionMapper;
import com.boms.modules.system.mapper.UserRoleMapper;
import com.boms.modules.system.entity.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 角色权限管理（M04）。 */
@Service
public class RoleService {

    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermMapper;
    private final PermissionMapper permissionMapper;
    private final UserRoleMapper userRoleMapper;

    public RoleService(RoleMapper roleMapper, RolePermissionMapper rolePermMapper,
                       PermissionMapper permissionMapper, UserRoleMapper userRoleMapper) {
        this.roleMapper = roleMapper;
        this.rolePermMapper = rolePermMapper;
        this.permissionMapper = permissionMapper;
        this.userRoleMapper = userRoleMapper;
    }

    public List<Role> list() {
        return roleMapper.selectList(new LambdaQueryWrapper<Role>().orderByAsc(Role::getId));
    }

    public List<Permission> allPermissions() {
        return permissionMapper.selectList(new LambdaQueryWrapper<Permission>().orderByAsc(Permission::getId));
    }

    public List<Long> permissionIdsOf(Long roleId) {
        return rolePermMapper.selectList(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId))
                .stream().map(RolePermission::getPermissionId).toList();
    }

    @Transactional
    public Role create(RoleReq req) {
        long dup = roleMapper.selectCount(new LambdaQueryWrapper<Role>().eq(Role::getCode, req.code()));
        if (dup > 0) throw new BizException(40901, "角色码已存在");
        Role r = new Role();
        r.setName(req.name());
        r.setCode(req.code());
        r.setDataScope(req.dataScope() == null ? "SELF" : req.dataScope());
        r.setStatus("ENABLED");
        r.setRemark(req.remark());
        roleMapper.insert(r);
        return r;
    }

    @Transactional
    public void update(Long id, RoleReq req) {
        Role r = require(id);
        r.setName(req.name());
        if (req.dataScope() != null) r.setDataScope(req.dataScope());
        r.setRemark(req.remark());
        roleMapper.updateById(r);
    }

    @Transactional
    public void delete(Long id) {
        require(id);
        long used = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, id));
        if (used > 0) throw new BizException(40902, "角色已分配给用户，无法删除");
        rolePermMapper.delete(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id));
        roleMapper.deleteById(id);
    }

    @Transactional
    public void assignPermissions(Long roleId, AssignPermReq req) {
        Role r = require(roleId);
        if (req.dataScope() != null) {
            r.setDataScope(req.dataScope());
            roleMapper.updateById(r);
        }
        rolePermMapper.delete(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
        if (req.permissionIds() != null) {
            for (Long pid : req.permissionIds()) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(pid);
                rolePermMapper.insert(rp);
            }
        }
    }

    private Role require(Long id) {
        Role r = roleMapper.selectById(id);
        if (r == null) throw new BizException(40400, "角色不存在或越权");
        return r;
    }
}
