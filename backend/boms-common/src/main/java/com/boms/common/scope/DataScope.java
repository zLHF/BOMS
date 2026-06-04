package com.boms.common.scope;

/**
 * 数据范围（角色 data_scope）。breadth 越大可见范围越广，多角色取最宽。
 * 对齐 04 权限码表 §3 / 决策 D3-D4。
 */
public enum DataScope {
    SELF(1),
    DEPT(2),
    DEPT_AND_SUB(3),
    TENANT(4),
    PLATFORM(5);

    public final int breadth;

    DataScope(int breadth) {
        this.breadth = breadth;
    }

    public static DataScope of(String v) {
        if (v == null) return SELF;
        try {
            return DataScope.valueOf(v);
        } catch (IllegalArgumentException e) {
            return SELF;
        }
    }

    /** 取更宽者。 */
    public static DataScope widest(DataScope a, DataScope b) {
        return a.breadth >= b.breadth ? a : b;
    }
}
