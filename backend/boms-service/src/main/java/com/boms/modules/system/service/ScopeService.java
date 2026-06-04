package com.boms.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.boms.common.scope.DataScope;
import com.boms.common.scope.ScopeFilter;
import com.boms.common.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.Set;

/** 根据当前用户角色的 data_scope 解析行级数据范围（M03/M04 基础，M05+ 复用）。 */
@Service
public class ScopeService {

    private final DeptService deptService;

    public ScopeService(DeptService deptService) {
        this.deptService = deptService;
    }

    public ScopeFilter current() {
        TenantContext.Principal p = TenantContext.get();
        if (p == null) return ScopeFilter.ofSelf();
        DataScope scope = DataScope.of(p.dataScope());
        return switch (scope) {
            case PLATFORM, TENANT -> ScopeFilter.ofAll();
            case DEPT_AND_SUB -> {
                Set<Long> ids = deptService.descendantIds(p.deptId());
                yield ids.isEmpty() ? ScopeFilter.ofSelf() : ScopeFilter.ofDepts(ids);
            }
            case DEPT -> p.deptId() == null ? ScopeFilter.ofSelf() : ScopeFilter.ofDepts(Set.of(p.deptId()));
            case SELF -> ScopeFilter.ofSelf();
        };
    }

    /**
     * 把当前用户的数据范围追加到查询：仅本人→owner=我；部门集→dept IN ids；全部→不加。
     * 业务列表（客户/商机/任务）通用。
     */
    public <T> void apply(LambdaQueryWrapper<T> w, SFunction<T, ?> ownerCol, SFunction<T, ?> deptCol) {
        ScopeFilter sf = current();
        if (sf.selfOnly()) {
            w.eq(ownerCol, TenantContext.userId());
        } else if (!sf.all()) {
            w.in(deptCol, sf.deptIds());
        }
    }
}
