package com.boms.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作权限校验：标在 Controller 方法上，由 {@link PermissionAspect} 校验当前用户是否持有权限码。
 * 缺权限 → 403。多个码默认「任一即可」，可设 requireAll=true 改为「全部需要」。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePerm {
    String[] value();

    boolean requireAll() default false;
}
