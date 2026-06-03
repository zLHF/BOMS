package com.boms.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boms.common.exception.BizException;
import com.boms.common.scope.ScopeFilter;
import com.boms.common.tenant.TenantContext;
import com.boms.modules.system.dto.UserCreateReq;
import com.boms.modules.system.dto.UserUpdateReq;
import com.boms.modules.system.entity.SysUser;
import com.boms.modules.system.entity.UserRole;
import com.boms.modules.system.mapper.SysUserMapper;
import com.boms.modules.system.mapper.UserRoleMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 用户管理（M03），列表按数据范围过滤。 */
@Service
public class UserService {

    private static final String DEFAULT_PWD = "123456";

    private final SysUserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final ScopeService scopeService;
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    public UserService(SysUserMapper userMapper, UserRoleMapper userRoleMapper, ScopeService scopeService) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.scopeService = scopeService;
    }

    public IPage<SysUser> list(String keyword, Long deptId, long page, long size) {
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.and(q -> q.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getMobile, keyword));
        }
        if (deptId != null) {
            w.eq(SysUser::getDeptId, deptId);
        }
        // 数据范围过滤（前端传参不可信，以服务端为准）
        ScopeFilter sf = scopeService.current();
        if (sf.selfOnly()) {
            w.eq(SysUser::getId, TenantContext.userId());
        } else if (!sf.all()) {
            w.in(SysUser::getDeptId, sf.deptIds());
        }
        w.orderByDesc(SysUser::getId);
        return userMapper.selectPage(new Page<>(page, size), w);
    }

    @Transactional
    public SysUser create(UserCreateReq req) {
        long dup = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.username()));
        if (dup > 0) throw new BizException(40901, "登录名已存在");

        SysUser u = new SysUser();
        u.setUsername(req.username());
        u.setMobile(req.mobile());
        u.setEmail(req.email());
        u.setRealName(req.realName());
        u.setDeptId(req.deptId());
        u.setDirectLeaderId(req.directLeaderId());
        u.setStatus("ENABLED");
        u.setMfaEnabled(0);
        String pwd = req.password() == null || req.password().isBlank() ? DEFAULT_PWD : req.password();
        u.setPasswordHash(bcrypt.encode(pwd));
        userMapper.insert(u);

        if (req.roleIds() != null) {
            assignRoles(u.getId(), req.roleIds());
        }
        return u;
    }

    @Transactional
    public void update(Long id, UserUpdateReq req) {
        SysUser u = requireUser(id);
        if (req.realName() != null) u.setRealName(req.realName());
        if (req.mobile() != null) u.setMobile(req.mobile());
        if (req.email() != null) u.setEmail(req.email());
        if (req.deptId() != null) u.setDeptId(req.deptId());
        if (req.directLeaderId() != null) u.setDirectLeaderId(req.directLeaderId());
        userMapper.updateById(u);
    }

    @Transactional
    public void disable(Long id) {
        SysUser u = requireUser(id);
        if (id.equals(TenantContext.userId())) throw new BizException(40904, "不能停用自己");
        u.setStatus("DISABLED");
        userMapper.updateById(u);
    }

    @Transactional
    public String resetPassword(Long id) {
        SysUser u = requireUser(id);
        u.setPasswordHash(bcrypt.encode(DEFAULT_PWD));
        userMapper.updateById(u);
        return DEFAULT_PWD;
    }

    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        requireUser(userId);
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (roleIds != null) {
            for (Long rid : roleIds) {
                UserRole ur = new UserRole();
                ur.setUserId(userId);
                ur.setRoleId(rid);
                userRoleMapper.insert(ur);
            }
        }
    }

    public List<Long> roleIdsOf(Long userId) {
        return userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId))
                .stream().map(UserRole::getRoleId).toList();
    }

    private SysUser requireUser(Long id) {
        SysUser u = userMapper.selectById(id);
        if (u == null) throw new BizException(40400, "用户不存在或越权");
        return u;
    }
}
