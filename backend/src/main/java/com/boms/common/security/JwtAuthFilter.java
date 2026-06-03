package com.boms.common.security;

import com.boms.common.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 入口过滤器：解析 Authorization Bearer token → 加载权限/角色/数据范围 → 写入 {@link TenantContext}，请求结束清理。
 * tenant_id 永远以 token 为准，前端不可信传入（02 架构 §8）。
 * 放行登录/健康检查等公开端点。
 */
@Slf4j
@Order(1)
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final PrincipalLoader principalLoader;

    public JwtAuthFilter(JwtUtil jwtUtil, PrincipalLoader principalLoader) {
        this.jwtUtil = jwtUtil;
        this.principalLoader = principalLoader;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/api/auth/login") || uri.startsWith("/api/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        try {
            String auth = req.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                try {
                    Claims c = jwtUtil.parse(auth.substring(7));
                    Long userId = Long.valueOf(c.getSubject());
                    Long tenantId = c.get("tid", Number.class).longValue();
                    String uname = c.get("uname", String.class);
                    // 先放最小上下文，使加载查询受租户拦截器保护，再换成完整上下文
                    TenantContext.set(new TenantContext.Principal(tenantId, userId, uname));
                    TenantContext.set(principalLoader.load(userId, tenantId, uname));
                } catch (Exception e) {
                    log.debug("token 解析/加载失败: {}", e.getMessage());
                    TenantContext.clear();
                }
            }
            chain.doFilter(req, resp);
        } finally {
            TenantContext.clear();
        }
    }
}
