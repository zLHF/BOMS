package com.boms.modules.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boms.common.audit.AuditLog;
import com.boms.common.result.R;
import com.boms.common.security.RequirePerm;
import com.boms.modules.system.dto.AssignRolesReq;
import com.boms.modules.system.dto.UserCreateReq;
import com.boms.modules.system.dto.UserUpdateReq;
import com.boms.modules.system.entity.SysUser;
import com.boms.modules.system.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 用户管理 M03。 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @RequirePerm("org:user:view")
    public R<IPage<SysUser>> list(@RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) Long deptId,
                                  @RequestParam(defaultValue = "1") long page,
                                  @RequestParam(defaultValue = "20") long size) {
        return R.ok(userService.list(keyword, deptId, page, size));
    }

    @GetMapping("/{id}/roles")
    @RequirePerm("org:user:view")
    public R<List<Long>> roles(@PathVariable Long id) {
        return R.ok(userService.roleIdsOf(id));
    }

    @PostMapping
    @RequirePerm("org:user:create")
    @AuditLog(action = "org:user:create", objectType = "sys_user")
    public R<Map<String, Object>> create(@Valid @RequestBody UserCreateReq req) {
        SysUser u = userService.create(req);
        return R.ok(Map.of("id", u.getId(), "username", u.getUsername()));
    }

    @PutMapping("/{id}")
    @RequirePerm("org:user:update")
    @AuditLog(action = "org:user:update", objectType = "sys_user")
    public R<Void> update(@PathVariable Long id, @RequestBody UserUpdateReq req) {
        userService.update(id, req);
        return R.ok();
    }

    @PatchMapping("/{id}/disable")
    @RequirePerm("org:user:disable")
    @AuditLog(action = "org:user:disable", objectType = "sys_user")
    public R<Void> disable(@PathVariable Long id) {
        userService.disable(id);
        return R.ok();
    }

    @PostMapping("/{id}/reset-password")
    @RequirePerm("org:user:reset_pwd")
    @AuditLog(action = "org:user:reset_pwd", objectType = "sys_user")
    public R<Map<String, Object>> resetPwd(@PathVariable Long id) {
        return R.ok(Map.of("password", userService.resetPassword(id)));
    }

    @PutMapping("/{id}/roles")
    @RequirePerm("org:user:assign_role")
    @AuditLog(action = "org:user:assign_role", objectType = "sys_user")
    public R<Void> assignRoles(@PathVariable Long id, @RequestBody AssignRolesReq req) {
        userService.assignRoles(id, req.roleIds());
        return R.ok();
    }
}
