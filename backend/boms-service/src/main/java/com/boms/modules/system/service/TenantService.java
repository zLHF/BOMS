package com.boms.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boms.common.exception.BizException;
import com.boms.common.tenant.TenantContext;
import com.boms.modules.system.dto.TenantCreateReq;
import com.boms.modules.system.dto.TenantUpdateReq;
import com.boms.modules.opportunity.entity.OpportunityStage;
import com.boms.modules.opportunity.mapper.OpportunityStageMapper;
import com.boms.modules.system.entity.*;
import com.boms.modules.system.mapper.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 租户管理（M02，平台级）。创建租户时引导默认角色矩阵 + 管理员账号。 */
@Service
public class TenantService {

    private static final Set<String> ACTIVE = Set.of("TRIAL", "NORMAL", "EXPIRING");
    private static final Set<String> ALL_STATUS = Set.of("TRIAL", "NORMAL", "EXPIRING", "FROZEN", "DISABLED");

    private final TenantMapper tenantMapper;
    private final SysPackageMapper packageMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermMapper;
    private final SysUserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final OpportunityStageMapper stageMapper;
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    public TenantService(TenantMapper tenantMapper, SysPackageMapper packageMapper, RoleMapper roleMapper,
                         PermissionMapper permissionMapper, RolePermissionMapper rolePermMapper,
                         SysUserMapper userMapper, UserRoleMapper userRoleMapper, OpportunityStageMapper stageMapper) {
        this.tenantMapper = tenantMapper;
        this.packageMapper = packageMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermMapper = rolePermMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.stageMapper = stageMapper;
    }

    public List<Tenant> list() {
        return tenantMapper.selectList(new LambdaQueryWrapper<Tenant>().orderByDesc(Tenant::getId));
    }

    public List<SysPackage> packages() {
        return packageMapper.selectList(null);
    }

    @Transactional
    public Map<String, Object> create(TenantCreateReq req) {
        if (tenantMapper.selectCount(new LambdaQueryWrapper<Tenant>().eq(Tenant::getCode, req.code())) > 0) {
            throw new BizException(40901, "租户编码已存在");
        }
        Tenant t = new Tenant();
        t.setName(req.name());
        t.setCode(req.code());
        t.setDomain(req.domain());
        t.setStatus("TRIAL");
        t.setPackageId(req.packageId());
        t.setExpireAt(parse(req.expireAt()));
        tenantMapper.insert(t);

        // 切换到新租户上下文，使引导的角色/用户落到新 tenant_id（当前是平台超管 tenant=0）
        TenantContext.Principal platform = TenantContext.get();
        Long opUser = platform == null ? null : platform.userId();
        try {
            TenantContext.set(new TenantContext.Principal(t.getId(), opUser, platform == null ? null : platform.username()));
            bootstrapRolesAndAdmin(t, req);
        } finally {
            TenantContext.set(platform);
        }

        Map<String, Object> out = new HashMap<>();
        out.put("tenantId", t.getId());
        out.put("code", t.getCode());
        out.put("adminUsername", req.adminUsername());
        out.put("adminPassword", req.adminPassword() == null || req.adminPassword().isBlank() ? "123456" : req.adminPassword());
        return out;
    }

    private void bootstrapRolesAndAdmin(Tenant t, TenantCreateReq req) {
        List<Permission> allPerms = permissionMapper.selectList(null);

        Long tenantAdminRoleId = null;
        for (RoleTemplate.Def def : RoleTemplate.DEFS) {
            Role r = new Role();
            r.setName(def.name());
            r.setCode(def.code());
            r.setDataScope(def.dataScope());
            r.setStatus("ENABLED");
            roleMapper.insert(r);          // tenant_id 由租户拦截器按新上下文注入
            if ("TENANT_ADMIN".equals(def.code())) tenantAdminRoleId = r.getId();

            for (Permission p : allPerms) {
                if (def.grants().test(p.getCode())) {
                    RolePermission rp = new RolePermission();
                    rp.setRoleId(r.getId());
                    rp.setPermissionId(p.getId());
                    rolePermMapper.insert(rp);
                }
            }
        }

        // 管理员账号
        SysUser admin = new SysUser();
        admin.setUsername(req.adminUsername());
        admin.setMobile(req.adminMobile());
        admin.setRealName(req.name() + "管理员");
        admin.setStatus("ENABLED");
        admin.setMfaEnabled(0);
        String pwd = req.adminPassword() == null || req.adminPassword().isBlank() ? "123456" : req.adminPassword();
        admin.setPasswordHash(bcrypt.encode(pwd));
        userMapper.insert(admin);

        if (tenantAdminRoleId != null) {
            UserRole ur = new UserRole();
            ur.setUserId(admin.getId());
            ur.setRoleId(tenantAdminRoleId);
            userRoleMapper.insert(ur);
        }

        seedStages();
    }

    /** D1 默认 7 阶段。 */
    private void seedStages() {
        String[][] defs = {
                {"prospecting", "初步接触", "1", "10", "gray", "IN_PROGRESS"},
                {"qualifying", "需求确认", "2", "30", "cyan", "IN_PROGRESS"},
                {"proposal", "方案报价", "3", "50", "blue", "IN_PROGRESS"},
                {"negotiation", "商务谈判", "4", "70", "orange", "IN_PROGRESS"},
                {"won", "赢单", "5", "100", "green", "WON"},
                {"lost", "输单", "6", "0", "red", "LOST"},
                {"void", "作废", "7", "0", "gray", "VOID"},
        };
        for (String[] d : defs) {
            OpportunityStage s = new OpportunityStage();
            s.setCode(d[0]);
            s.setName(d[1]);
            s.setSort(Integer.parseInt(d[2]));
            s.setWinRate(Integer.parseInt(d[3]));
            s.setColor(d[4]);
            s.setStageType(d[5]);
            s.setIsActive(1);
            stageMapper.insert(s);
        }
    }

    @Transactional
    public void update(Long id, TenantUpdateReq req) {
        Tenant t = require(id);
        if (req.name() != null) t.setName(req.name());
        if (req.domain() != null) t.setDomain(req.domain());
        if (req.expireAt() != null) t.setExpireAt(parse(req.expireAt()));
        tenantMapper.updateById(t);
    }

    @Transactional
    public void changeStatus(Long id, String status) {
        if (!ALL_STATUS.contains(status)) throw new BizException(40001, "非法状态: " + status);
        Tenant t = require(id);
        t.setStatus(status);
        tenantMapper.updateById(t);
    }

    @Transactional
    public void changePackage(Long id, Long packageId) {
        if (packageMapper.selectById(packageId) == null) throw new BizException(40400, "套餐不存在");
        Tenant t = require(id);
        t.setPackageId(packageId);
        tenantMapper.updateById(t);
    }

    private Tenant require(Long id) {
        Tenant t = tenantMapper.selectById(id);
        if (t == null) throw new BizException(40400, "租户不存在");
        return t;
    }

    private LocalDateTime parse(String s) {
        if (s == null || s.isBlank()) return null;
        String v = s.trim();
        if (v.length() == 10) v = v + " 00:00:00";
        return LocalDateTime.parse(v, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
