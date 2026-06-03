package com.boms.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计注解：标在 Controller/Service 方法上，由 {@link AuditAspect} 落 audit_log。
 * PRD M18：记录操作人/时间/IP/对象/动作（前后值在 V1.0 关键写操作上逐步补全）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {
    /** 语义动作或权限码，如 "tenant:create"、"login"。 */
    String action();

    /** 对象类型，如 "tenant"、"opportunity"。 */
    String objectType() default "";
}
