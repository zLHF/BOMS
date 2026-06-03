package com.boms.modules.system;

import com.boms.common.audit.AuditLog;
import com.boms.common.result.R;
import com.boms.common.security.RequirePerm;
import com.boms.modules.system.dto.AssignPermReq;
import com.boms.modules.system.dto.RoleReq;
import com.boms.modules.system.entity.Permission;
import com.boms.modules.system.entity.Role;
import com.boms.modules.system.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 角色权限 M04。 */
@RestController
@RequestMapping("/api")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/roles")
    @RequirePerm("role:view")
    public R<List<Role>> list() {
        return R.ok(roleService.list());
    }

    @GetMapping("/roles/{id}/permissions")
    @RequirePerm("role:view")
    public R<List<Long>> rolePerms(@PathVariable Long id) {
        return R.ok(roleService.permissionIdsOf(id));
    }

    /** 权限码字典（登录态即可，供角色配置页渲染）。 */
    @GetMapping("/permissions")
    public R<List<Permission>> permissions() {
        return R.ok(roleService.allPermissions());
    }

    @PostMapping("/roles")
    @RequirePerm("role:create")
    @AuditLog(action = "role:create", objectType = "role")
    public R<Role> create(@Valid @RequestBody RoleReq req) {
        return R.ok(roleService.create(req));
    }

    @PutMapping("/roles/{id}")
    @RequirePerm("role:update")
    @AuditLog(action = "role:update", objectType = "role")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody RoleReq req) {
        roleService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/roles/{id}")
    @RequirePerm("role:delete")
    @AuditLog(action = "role:delete", objectType = "role")
    public R<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return R.ok();
    }

    @PutMapping("/roles/{id}/permissions")
    @RequirePerm("role:assign_perm")
    @AuditLog(action = "role:assign_perm", objectType = "role")
    public R<Void> assignPerms(@PathVariable Long id, @RequestBody AssignPermReq req) {
        roleService.assignPermissions(id, req);
        return R.ok();
    }
}
