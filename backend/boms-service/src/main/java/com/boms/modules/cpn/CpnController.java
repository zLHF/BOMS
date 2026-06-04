package com.boms.modules.cpn;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boms.common.exception.BizException;
import com.boms.common.result.R;
import com.boms.common.security.JwtUtil;
import com.boms.modules.cpn.dto.CpnBaseResp;
import com.boms.modules.cpn.dto.CpnProvisionData;
import com.boms.modules.cpn.dto.CpnProvisionReq;
import com.boms.modules.cpn.entity.CpnTenantMapping;
import com.boms.modules.cpn.entity.CpnUserMapping;
import com.boms.modules.cpn.mapper.CpnTenantMappingMapper;
import com.boms.modules.cpn.mapper.CpnUserMappingMapper;
import com.boms.modules.system.entity.SysUser;
import com.boms.modules.system.entity.Tenant;
import com.boms.modules.system.mapper.SysUserMapper;
import com.boms.modules.system.mapper.TenantMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 算力平台对接 Controller。 */
@Slf4j
@RestController
public class CpnController {

    private final CpnConfig config;
    private final CpnAuthService cpnAuthService;
    private final TenantMapper tenantMapper;
    private final SysUserMapper userMapper;
    private final CpnTenantMappingMapper tenantMappingMapper;
    private final CpnUserMappingMapper userMappingMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    public CpnController(CpnConfig config, CpnAuthService cpnAuthService,
                         TenantMapper tenantMapper, SysUserMapper userMapper,
                         CpnTenantMappingMapper tenantMappingMapper,
                         CpnUserMappingMapper userMappingMapper, JwtUtil jwtUtil) {
        this.config = config;
        this.cpnAuthService = cpnAuthService;
        this.tenantMapper = tenantMapper;
        this.userMapper = userMapper;
        this.tenantMappingMapper = tenantMappingMapper;
        this.userMappingMapper = userMappingMapper;
        this.jwtUtil = jwtUtil;
    }

    /* ==================== 被调方：用户开通接口 ==================== */

    /**
     * 算力平台调用此接口开通用户。
     * URL: POST /open-api/app/tenants
     */
    @PostMapping("/open-api/app/tenants")
    public CpnBaseResp provision(@RequestBody CpnProvisionReq req) {
        // 1. 校验 appKey/appSecret
        if (!config.getAppKey().equals(req.getAppKey()) || !config.getAppSecret().equals(req.getAppSecret())) {
            log.warn("[CPN] 用户开通鉴权失败: appKey={}", req.getAppKey());
            return CpnBaseResp.fail(30001, "应用鉴权失败");
        }

        try {
            // 2. 查找或创建租户
            Tenant tenant = findOrCreateTenant(req.getEnterpriseName());

            // 3. 查找或创建用户
            SysUser user = findOrCreateUser(tenant.getId(), req);

            // 4. 写入映射
            ensureMapping(tenant.getId(), req.getEnterpriseName(), user.getId(), req.getPltAccountLogin(), req.getPltUserCn());

            // 5. 生成 BOMS token
            String token = jwtUtil.issue(user.getId(), tenant.getId(), user.getUsername());

            log.info("[CPN] 用户开通成功: enterprise={} account={} tenant={}", req.getEnterpriseName(), req.getPltAccountLogin(), tenant.getId());
            return CpnBaseResp.ok(new CpnProvisionData(
                    user.getUsername(),
                    token,
                    String.valueOf(tenant.getId())
            ));
        } catch (Exception e) {
            log.error("[CPN] 用户开通失败: {}", e.getMessage(), e);
            return CpnBaseResp.fail(50000, "开通失败: " + e.getMessage());
        }
    }

    /* ==================== 主动调方：SSO 端点 ==================== */

    /**
     * 前端 SSO 入口：用 code 换 BOMS token。
     * URL: GET /api/cpn/sso?code=xxx
     */
    @GetMapping("/api/cpn/sso")
    public R<CpnAuthService.SsoResult> sso(@RequestParam String code) {
        return R.ok(cpnAuthService.ssoLogin(code));
    }

    /* ==================== 内部方法 ==================== */

    @Transactional
    protected Tenant findOrCreateTenant(String enterpriseName) {
        // 先查映射表
        CpnTenantMapping mapping = tenantMappingMapper.selectOne(
                new LambdaQueryWrapper<CpnTenantMapping>().eq(CpnTenantMapping::getCpnEnterpriseName, enterpriseName));
        if (mapping != null) {
            return tenantMapper.selectById(mapping.getBomsTenantId());
        }

        // 查现有租户（按名称模糊匹配）
        Tenant existing = tenantMapper.selectOne(
                new LambdaQueryWrapper<Tenant>().eq(Tenant::getName, enterpriseName));
        if (existing != null) {
            CpnTenantMapping m = new CpnTenantMapping();
            m.setBomsTenantId(existing.getId());
            m.setCpnEnterpriseName(enterpriseName);
            tenantMappingMapper.insert(m);
            return existing;
        }

        // 创建新租户
        Tenant t = new Tenant();
        t.setName(enterpriseName);
        t.setCode("cpn_" + System.currentTimeMillis());
        t.setStatus("NORMAL");
        tenantMapper.insert(t);

        CpnTenantMapping m = new CpnTenantMapping();
        m.setBomsTenantId(t.getId());
        m.setCpnEnterpriseName(enterpriseName);
        tenantMappingMapper.insert(m);

        log.info("[CPN] 自动创建租户: name={} id={}", enterpriseName, t.getId());
        return t;
    }

    @Transactional
    protected SysUser findOrCreateUser(Long tenantId, CpnProvisionReq req) {
        // 先查映射表
        CpnUserMapping mapping = userMappingMapper.selectOne(
                new LambdaQueryWrapper<CpnUserMapping>().eq(CpnUserMapping::getCpnLoginName, req.getPltAccountLogin()));
        if (mapping != null) {
            return userMapper.selectById(mapping.getBomsUserId());
        }

        // 查现有用户
        SysUser existing = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getTenantId, tenantId)
                        .eq(SysUser::getUsername, req.getPltAccountLogin()));
        if (existing != null) {
            CpnUserMapping m = new CpnUserMapping();
            m.setBomsUserId(existing.getId());
            m.setCpnLoginName(req.getPltAccountLogin());
            m.setCpnUserCn(req.getPltUserCn());
            userMappingMapper.insert(m);
            return existing;
        }

        // 创建新用户
        SysUser u = new SysUser();
        u.setTenantId(tenantId);
        u.setUsername(req.getPltAccountLogin());
        u.setRealName(req.getPltUserCn() != null ? req.getPltUserCn() : req.getPltAccountLogin());
        u.setMobile(req.getPltMobile());
        u.setEmail(req.getPltEmail());
        u.setPasswordHash(bcrypt.encode("Boms@" + System.currentTimeMillis())); // 随机初始密码
        u.setStatus("ENABLED");
        userMapper.insert(u);

        CpnUserMapping m = new CpnUserMapping();
        m.setBomsUserId(u.getId());
        m.setCpnLoginName(req.getPltAccountLogin());
        m.setCpnUserCn(req.getPltUserCn());
        userMappingMapper.insert(m);

        log.info("[CPN] 自动创建用户: login={} id={}", req.getPltAccountLogin(), u.getId());
        return u;
    }

    private void ensureMapping(Long tenantId, String enterpriseName, Long userId, String loginName, String userCn) {
        // 确保租户映射存在
        CpnTenantMapping tm = tenantMappingMapper.selectOne(
                new LambdaQueryWrapper<CpnTenantMapping>().eq(CpnTenantMapping::getBomsTenantId, tenantId));
        if (tm == null) {
            tm = new CpnTenantMapping();
            tm.setBomsTenantId(tenantId);
            tm.setCpnEnterpriseName(enterpriseName);
            tenantMappingMapper.insert(tm);
        }

        // 确保用户映射存在
        CpnUserMapping um = userMappingMapper.selectOne(
                new LambdaQueryWrapper<CpnUserMapping>().eq(CpnUserMapping::getBomsUserId, userId));
        if (um == null) {
            um = new CpnUserMapping();
            um.setBomsUserId(userId);
            um.setCpnLoginName(loginName);
            um.setCpnUserCn(userCn);
            userMappingMapper.insert(um);
        }
    }
}
