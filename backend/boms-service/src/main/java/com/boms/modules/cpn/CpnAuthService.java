package com.boms.modules.cpn;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boms.common.exception.BizException;
import com.boms.common.security.JwtUtil;
import com.boms.modules.cpn.entity.CpnTenantMapping;
import com.boms.modules.cpn.entity.CpnUserMapping;
import com.boms.modules.cpn.mapper.CpnTenantMappingMapper;
import com.boms.modules.cpn.mapper.CpnUserMappingMapper;
import com.boms.modules.system.entity.SysUser;
import com.boms.modules.system.entity.Tenant;
import com.boms.modules.system.mapper.SysUserMapper;
import com.boms.modules.system.mapper.TenantMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** 算力平台对接认证服务。 */
@Slf4j
@Service
public class CpnAuthService {

    private final CpnConfig config;
    private final CpnClient cpnClient;
    private final CpnTenantMappingMapper tenantMappingMapper;
    private final CpnUserMappingMapper userMappingMapper;
    private final TenantMapper tenantMapper;
    private final SysUserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    public CpnAuthService(CpnConfig config, CpnClient cpnClient,
                          CpnTenantMappingMapper tenantMappingMapper,
                          CpnUserMappingMapper userMappingMapper,
                          TenantMapper tenantMapper, SysUserMapper userMapper, JwtUtil jwtUtil) {
        this.config = config;
        this.cpnClient = cpnClient;
        this.tenantMappingMapper = tenantMappingMapper;
        this.userMappingMapper = userMappingMapper;
        this.tenantMapper = tenantMapper;
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    /**
     * SSO 登录流程：用 code 换 token → 获取用户信息 → 匹配本地用户 → 返回 BOMS token。
     */
    public SsoResult ssoLogin(String code) {
        // 1. 身份校验
        String cpnToken = cpnClient.authCheck(code);
        if (cpnToken == null) {
            throw new BizException(40101, "算力平台身份校验失败");
        }

        // 2. 获取用户信息
        JsonNode userInfo = cpnClient.getUserInfoByToken(cpnToken);
        if (userInfo == null) {
            throw new BizException(40101, "获取算力平台用户信息失败");
        }

        // 3. 从用户信息中提取租户和用户标识
        String loginName = userInfo.path("user").path("loginLoginname").asText("");
        String loginCn = userInfo.path("user").path("loginUname").asText("");
        String cpnTenantId = userInfo.path("tenant").path("tenantID").asText("");
        String cpnTenantAccount = userInfo.path("tenant").path("tenantAccount").asText("");

        if (loginName.isEmpty()) {
            throw new BizException(40101, "算力平台用户信息不完整");
        }

        // 4. 查找本地用户映射
        CpnUserMapping mapping = userMappingMapper.selectOne(
                new LambdaQueryWrapper<CpnUserMapping>().eq(CpnUserMapping::getCpnLoginName, loginName));

        if (mapping == null) {
            throw new BizException(40101, "该算力平台用户未开通 BOMS 账号，请先通过平台开通");
        }

        SysUser user = userMapper.selectById(mapping.getBomsUserId());
        if (user == null) {
            throw new BizException(40101, "关联的 BOMS 用户不存在");
        }

        // 5. 更新映射中的平台租户信息
        if (!cpnTenantId.isEmpty()) {
            CpnTenantMapping tm = tenantMappingMapper.selectOne(
                    new LambdaQueryWrapper<CpnTenantMapping>().eq(CpnTenantMapping::getBomsTenantId, user.getTenantId()));
            if (tm != null && (tm.getCpnTenantId() == null || !tm.getCpnTenantId().equals(cpnTenantId))) {
                tm.setCpnTenantId(cpnTenantId);
                tenantMappingMapper.updateById(tm);
            }
        }

        // 6. 生成 BOMS token
        String token = jwtUtil.issue(user.getId(), user.getTenantId(), user.getUsername());
        log.info("[CPN-SSO] 算力平台用户 {} 映射到 BOMS 用户 {} (tenant={})", loginName, user.getUsername(), user.getTenantId());
        return new SsoResult(token, user.getId(), user.getTenantId(), user.getUsername(), user.getRealName());
    }

    public record SsoResult(String token, Long userId, Long tenantId, String username, String realName) {}
}
