package com.boms.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boms.modules.system.entity.Role;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface RoleMapper extends BaseMapper<Role> {

    /** 取某用户的角色（含 data_scope）。租户隔离由拦截器对 user_role/role 追加。 */
    @Select("""
            SELECT r.* FROM role r
            JOIN user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId} AND r.deleted = 0
            """)
    List<Role> selectByUserId(@Param("userId") Long userId);
}
