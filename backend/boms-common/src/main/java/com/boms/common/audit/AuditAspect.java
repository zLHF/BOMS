package com.boms.common.audit;

import com.boms.common.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/** @AuditLog 切面：方法成功/失败均落 audit_log，失败标 DENIED/ERROR。 */
@Slf4j
@Aspect
@Component
public class AuditAspect {

    private final AuditLogMapper auditLogMapper;

    public AuditAspect(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint pjp, AuditLog auditLog) throws Throwable {
        String result = "SUCCESS";
        try {
            return pjp.proceed();
        } catch (Throwable t) {
            result = "ERROR";
            throw t;
        } finally {
            try {
                save(auditLog, result, ((MethodSignature) pjp.getSignature()).getMethod().getName());
            } catch (Exception e) {
                log.warn("写审计日志失败: {}", e.getMessage());
            }
        }
    }

    private void save(AuditLog a, String result, String method) {
        TenantContext.Principal p = TenantContext.get();
        AuditLogEntity e = new AuditLogEntity();
        e.setTenantId(p == null || p.tenantId() == null ? 0L : p.tenantId());
        e.setUserId(p == null ? null : p.userId());
        e.setUserName(p == null ? null : p.username());
        e.setObjectType(a.objectType().isEmpty() ? null : a.objectType());
        e.setAction(a.action().isEmpty() ? method : a.action());
        e.setResult(result);
        e.setCreatedAt(LocalDateTime.now());

        HttpServletRequest req = currentRequest();
        if (req != null) {
            e.setIp(clientIp(req));
            String ua = req.getHeader("User-Agent");
            e.setUserAgent(ua != null && ua.length() > 255 ? ua.substring(0, 255) : ua);
        }
        auditLogMapper.insert(e);
    }

    private HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        return attrs instanceof ServletRequestAttributes sra ? sra.getRequest() : null;
    }

    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
