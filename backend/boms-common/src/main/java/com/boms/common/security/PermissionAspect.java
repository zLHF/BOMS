package com.boms.common.security;

import com.boms.common.exception.ForbiddenException;
import com.boms.common.exception.UnauthorizedException;
import com.boms.common.tenant.TenantContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/** @RequirePerm 校验：缺登录态 → 401，缺权限码 → 403。 */
@Aspect
@Component
public class PermissionAspect {

    @Before("@annotation(requirePerm)")
    public void check(RequirePerm requirePerm) {
        TenantContext.Principal p = TenantContext.get();
        if (p == null) {
            throw new UnauthorizedException("未登录");
        }
        String[] need = requirePerm.value();
        boolean ok = requirePerm.requireAll()
                ? Arrays.stream(need).allMatch(c -> p.permissions().contains(c))
                : Arrays.stream(need).anyMatch(c -> p.permissions().contains(c));
        if (!ok) {
            throw new ForbiddenException("无权限：需要 " + String.join(
                    requirePerm.requireAll() ? " 且 " : " 或 ", need));
        }
    }
}
