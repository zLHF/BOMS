package com.boms.modules.system.service;

import java.util.List;
import java.util.function.Predicate;

/** 新租户默认角色 + 权限矩阵模板（对齐 10-数据初始化 ⑤、04 §6）。 */
public final class RoleTemplate {

    public record Def(String name, String code, String dataScope, Predicate<String> grants) {}

    private RoleTemplate() {}

    private static final List<String> SALES_MANAGER = List.of(
            "menu:dashboard", "menu:opportunity", "menu:customer", "menu:task", "menu:follow",
            "opp:view", "opp:view:sub", "opp:create", "opp:update", "opp:transfer", "opp:import", "opp:export",
            "opp:follow:create", "opp:stage:advance", "opp:stage:rollback", "opp:win", "opp:lose",
            "opp:collab:add", "opp:collab:remove",
            "customer:view", "customer:view:sub", "customer:create", "customer:update", "customer:transfer",
            "customer:contact:manage", "customer:child:manage", "customer:import", "customer:export", "customer:follow:create",
            "task:view", "task:view:sub", "task:create", "task:update", "task:assign", "task:cancel",
            "file:upload", "file:download", "file:delete");

    private static final List<String> SALES = List.of(
            "menu:dashboard", "menu:opportunity", "menu:customer", "menu:task", "menu:follow",
            "opp:view", "opp:create", "opp:update", "opp:follow:create", "opp:stage:advance", "opp:win", "opp:lose",
            "opp:collab:add", "opp:collab:remove",
            "customer:view", "customer:create", "customer:update", "customer:contact:manage", "customer:child:manage", "customer:follow:create",
            "task:view", "task:create", "task:update", "task:assign", "task:cancel",
            "file:upload", "file:download");

    private static final List<String> AUDITOR = List.of(
            "menu:dashboard", "menu:settings:audit", "opp:view", "customer:view", "audit:view", "audit:export");

    /** 租户管理员：本租户全部码（排除平台码）。 */
    private static boolean isTenantAdminCode(String code) {
        return !code.startsWith("tenant:")
                && !code.startsWith("platform:")
                && !code.equals("menu:platform:tenant");
    }

    public static final List<Def> DEFS = List.of(
            new Def("租户管理员", "TENANT_ADMIN", "TENANT", RoleTemplate::isTenantAdminCode),
            new Def("销售主管", "SALES_MANAGER", "DEPT_AND_SUB", SALES_MANAGER::contains),
            new Def("销售人员", "SALES", "SELF", SALES::contains),
            new Def("只读审计员", "AUDITOR", "TENANT", AUDITOR::contains)
    );
}
