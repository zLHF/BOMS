package com.boms.common.scope;

import java.util.Set;

/**
 * 数据范围解析结果。
 * @param all      true=租户内全部（由租户拦截器保证不跨租户）
 * @param deptIds  限定的部门 ID 集合（all=false 且非 selfOnly 时生效）
 * @param selfOnly true=仅本人
 */
public record ScopeFilter(boolean all, Set<Long> deptIds, boolean selfOnly) {
    public static ScopeFilter ofAll() {
        return new ScopeFilter(true, Set.of(), false);
    }
    public static ScopeFilter ofDepts(Set<Long> deptIds) {
        return new ScopeFilter(false, deptIds, false);
    }
    public static ScopeFilter ofSelf() {
        return new ScopeFilter(false, Set.of(), true);
    }
}
