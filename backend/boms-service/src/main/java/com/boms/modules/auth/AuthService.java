package com.boms.modules.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boms.common.exception.BizException;
import com.boms.common.security.JwtUtil;
import com.boms.modules.auth.dto.LoginRequest;
import com.boms.modules.auth.dto.LoginResponse;
import com.boms.modules.system.entity.LoginLog;
import com.boms.modules.system.entity.SysUser;
import com.boms.modules.system.entity.Tenant;
import com.boms.modules.system.mapper.LoginLogMapper;
import com.boms.modules.system.mapper.PermissionMapper;
import com.boms.modules.system.mapper.SysUserMapper;
import com.boms.modules.system.mapper.TenantMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class AuthService {

    private final SysUserMapper userMapper;
    private final TenantMapper tenantMapper;
    private final PermissionMapper permissionMapper;
    private final LoginLogMapper loginLogMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Value("${boms.demo.allow-plain-password:false}")
    private boolean allowPlain;
    @Value("${boms.demo.plain-password:123456}")
    private String plainPassword;

    public AuthService(SysUserMapper userMapper, TenantMapper tenantMapper,
                       PermissionMapper permissionMapper, LoginLogMapper loginLogMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.tenantMapper = tenantMapper;
        this.permissionMapper = permissionMapper;
        this.loginLogMapper = loginLogMapper;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(LoginRequest req) {
        Long tenantId = resolveTenantId(req.getTenantCode());
        SysUser user = findUser(tenantId, req.getAccount());
        if (user == null) {
            writeLoginLog(tenantId, null, req.getAccount(), "FAIL", "账号不存在");
            throw new BizException(40101, "账号或密码错误");
        }
        if (!"ENABLED".equals(user.getStatus())) {
            writeLoginLog(tenantId, user.getId(), user.getUsername(), "LOCKED", "状态=" + user.getStatus());
            throw new BizException(40102, "账号已停用");
        }
        if (!verifyPassword(req.getPassword(), user.getPasswordHash())) {
            writeLoginLog(tenantId, user.getId(), user.getUsername(), "FAIL", "密码错误");
            throw new BizException(40101, "账号或密码错误");
        }

        List<String> perms = permissionMapper.selectCodesByUserId(user.getId());
        String token = jwtUtil.issue(user.getId(), tenantId, user.getUsername());
        writeLoginLog(tenantId, user.getId(), user.getUsername(), "SUCCESS", null);
        log.info("登录成功 tenant={} user={} perms={}", tenantId, user.getUsername(), perms.size());
        return new LoginResponse(token, user.getId(), tenantId, user.getUsername(), user.getRealName(), perms);
    }

    private Long resolveTenantId(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            return 0L; // 平台超管
        }
        Tenant t = tenantMapper.selectOne(
                new LambdaQueryWrapper<Tenant>().eq(Tenant::getCode, tenantCode));
        if (t == null) {
            throw new BizException(40103, "租户不存在: " + tenantCode);
        }
        if (!"NORMAL".equals(t.getStatus()) && !"TRIAL".equals(t.getStatus())) {
            throw new BizException(40104, "租户状态不可用: " + t.getStatus());
        }
        return t.getId();
    }

    private SysUser findUser(Long tenantId, String account) {
        // 登录阶段 TenantContext 为空，租户拦截器不追加条件；此处显式按 tenant_id 限定。
        return userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getTenantId, tenantId)
                .and(w -> w.eq(SysUser::getUsername, account).or().eq(SysUser::getMobile, account))
                .last("limit 1"));
    }

    private boolean verifyPassword(String raw, String hash) {
        if (allowPlain && plainPassword.equals(raw)) {
            return true; // 阶段 0：种子 password_hash 为占位，放行演示明文
        }
        try {
            return hash != null && bcrypt.matches(raw, hash);
        } catch (Exception e) {
            return false;
        }
    }

    private void writeLoginLog(Long tenantId, Long userId, String username, String result, String failReason) {
        try {
            LoginLog lg = new LoginLog();
            lg.setTenantId(tenantId == null ? 0L : tenantId);
            lg.setUserId(userId);
            lg.setUsername(username);
            lg.setResult(result);
            lg.setFailReason(failReason);
            lg.setCreatedAt(LocalDateTime.now());
            HttpServletRequest req = currentRequest();
            if (req != null) {
                lg.setIp(req.getRemoteAddr());
                lg.setDevice(req.getHeader("User-Agent"));
            }
            loginLogMapper.insert(lg);
        } catch (Exception e) {
            log.warn("写登录日志失败: {}", e.getMessage());
        }
    }

    private HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        return attrs instanceof ServletRequestAttributes sra ? sra.getRequest() : null;
    }
}
