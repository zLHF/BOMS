package com.boms.modules.auth;

import com.boms.common.audit.AuditLog;
import com.boms.common.exception.BizException;
import com.boms.common.exception.UnauthorizedException;
import com.boms.common.result.R;
import com.boms.common.tenant.TenantContext;
import com.boms.modules.auth.dto.ChangePwdReq;
import com.boms.modules.system.entity.SysUser;
import com.boms.modules.system.mapper.SysUserMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/** 当前账号：me / 改密 / 退出（M01）。 */
@RestController
@RequestMapping("/api/auth")
public class AccountController {

    private final SysUserMapper userMapper;
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Value("${boms.demo.allow-plain-password:false}")
    private boolean allowPlain;
    @Value("${boms.demo.plain-password:123456}")
    private String plainPassword;

    public AccountController(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /** 当前用户 + 菜单/操作权限码 + 角色 + 数据范围（驱动前端菜单与 v-perm）。 */
    @GetMapping("/me")
    public R<Map<String, Object>> me() {
        TenantContext.Principal p = TenantContext.get();
        if (p == null) throw new UnauthorizedException("未登录");
        SysUser u = userMapper.selectById(p.userId());
        Map<String, Object> data = new HashMap<>();
        data.put("userId", p.userId());
        data.put("tenantId", p.tenantId());
        data.put("username", p.username());
        data.put("realName", u == null ? p.username() : u.getRealName());
        data.put("isPlatform", TenantContext.isPlatform());
        data.put("dataScope", p.dataScope());
        data.put("roles", p.roleCodes());
        data.put("permissions", p.permissions());
        return R.ok(data);
    }

    @PostMapping("/change-password")
    @AuditLog(action = "change-password", objectType = "sys_user")
    public R<Void> changePassword(@Valid @RequestBody ChangePwdReq req) {
        TenantContext.Principal p = TenantContext.get();
        if (p == null) throw new UnauthorizedException("未登录");
        SysUser u = userMapper.selectById(p.userId());
        if (u == null) throw new BizException(40400, "用户不存在");
        boolean ok = (allowPlain && plainPassword.equals(req.oldPassword()))
                || bcrypt.matches(req.oldPassword(), u.getPasswordHash());
        if (!ok) throw new BizException(40105, "原密码错误");
        if (req.newPassword().length() < 6) throw new BizException(40001, "新密码至少 6 位");
        u.setPasswordHash(bcrypt.encode(req.newPassword()));
        userMapper.updateById(u);
        return R.ok();
    }

    @PostMapping("/logout")
    @AuditLog(action = "logout", objectType = "auth")
    public R<Void> logout() {
        // 无状态 JWT：服务端不维护会话，前端清 token 即可。审计记录退出动作。
        return R.ok();
    }
}
