package com.boms.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boms.modules.system.entity.Permission;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PermissionMapper extends BaseMapper<Permission> {

    /** 取某用户的全部权限码（经 角色→角色权限→权限码）。租户隔离由租户拦截器对 user_role/role_permission 追加。 */
    @Select("""
            SELECT DISTINCT p.code
            FROM permission p
            JOIN role_permission rp ON rp.permission_id = p.id
            JOIN user_role ur ON ur.role_id = rp.role_id
            WHERE ur.user_id = #{userId}
            """)
    List<String> selectCodesByUserId(@Param("userId") Long userId);
}
