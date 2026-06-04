package com.boms.modules.opportunity;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boms.common.exception.BizException;
import com.boms.common.exception.ForbiddenException;
import com.boms.common.tenant.TenantContext;
import com.boms.modules.customer.entity.Contact;
import com.boms.modules.customer.entity.Customer;
import com.boms.modules.customer.mapper.ContactMapper;
import com.boms.modules.customer.mapper.CustomerMapper;
import com.boms.modules.opportunity.dto.*;
import com.boms.modules.opportunity.entity.Opportunity;
import com.boms.modules.opportunity.entity.OpportunityCollaborator;
import com.boms.modules.opportunity.entity.OpportunityFollow;
import com.boms.modules.opportunity.entity.OpportunityStage;
import com.boms.modules.opportunity.mapper.OpportunityCollaboratorMapper;
import com.boms.modules.opportunity.mapper.OpportunityFollowMapper;
import com.boms.modules.opportunity.mapper.OpportunityMapper;
import com.boms.modules.opportunity.mapper.OpportunityStageMapper;
import com.boms.modules.system.entity.SysUser;
import com.boms.modules.system.mapper.SysUserMapper;
import com.boms.modules.system.service.ScopeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

/** 商机 M05/M06/M07：多视图列表 + 新增/编辑 + 阶段推进/成交/输单/回退 + 跟进。 */
@Service
public class OpportunityService {

    private static final Set<String> ROLLBACK_ROLES = Set.of("SALES_MANAGER", "TENANT_ADMIN");

    private final OpportunityMapper oppMapper;
    private final OpportunityStageMapper stageMapper;
    private final OpportunityFollowMapper followMapper;
    private final OpportunityCollaboratorMapper collabMapper;
    private final CustomerMapper customerMapper;
    private final ContactMapper contactMapper;
    private final SysUserMapper userMapper;
    private final ScopeService scopeService;

    public OpportunityService(OpportunityMapper oppMapper, OpportunityStageMapper stageMapper,
                              OpportunityFollowMapper followMapper, OpportunityCollaboratorMapper collabMapper,
                              CustomerMapper customerMapper, ContactMapper contactMapper,
                              SysUserMapper userMapper, ScopeService scopeService) {
        this.oppMapper = oppMapper;
        this.stageMapper = stageMapper;
        this.followMapper = followMapper;
        this.collabMapper = collabMapper;
        this.customerMapper = customerMapper;
        this.contactMapper = contactMapper;
        this.userMapper = userMapper;
        this.scopeService = scopeService;
    }

    /* ---------- 阶段字典 ---------- */

    public List<OpportunityStage> stages() {
        return stageMapper.selectList(new LambdaQueryWrapper<OpportunityStage>()
                .eq(OpportunityStage::getIsActive, 1).orderByAsc(OpportunityStage::getSort));
    }

    private OpportunityStage firstStage() {
        return stages().stream().filter(s -> "IN_PROGRESS".equals(s.getStageType())).findFirst()
                .orElseThrow(() -> new BizException(50001, "未配置商机阶段，请先初始化租户阶段"));
    }

    private OpportunityStage stageOfType(String type) {
        return stages().stream().filter(s -> type.equals(s.getStageType())).findFirst()
                .orElseThrow(() -> new BizException(50001, "未配置 " + type + " 阶段"));
    }

    private OpportunityStage requireStage(Long id) {
        OpportunityStage s = stageMapper.selectById(id);
        if (s == null) throw new BizException(40400, "阶段不存在");
        return s;
    }

    /* ---------- 列表（7 视图核心子集） ---------- */

    public IPage<Opportunity> list(String view, String keyword, Long stageId, long page, long size) {
        LambdaQueryWrapper<Opportunity> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) w.like(Opportunity::getTitle, keyword);
        if (stageId != null) w.eq(Opportunity::getStageId, stageId);

        String v = view == null ? "all" : view;
        switch (v) {
            case "mine" -> w.eq(Opportunity::getOwnerId, TenantContext.userId());
            case "won" -> {
                w.eq(Opportunity::getStatus, "WON");
                scopeService.apply(w, Opportunity::getOwnerId, Opportunity::getDeptId);
            }
            case "sub" -> {
                if (!TenantContext.hasPerm("opp:view:sub")) throw new ForbiddenException("无下属数据查看权限");
                scopeService.apply(w, Opportunity::getOwnerId, Opportunity::getDeptId);
            }
            case "pool" -> w.eq(Opportunity::getIsPool, 1);
            default -> scopeService.apply(w, Opportunity::getOwnerId, Opportunity::getDeptId);
        }
        w.orderByDesc(Opportunity::getUpdatedAt);
        return oppMapper.selectPage(new Page<>(page, size), w);
    }

    public Opportunity get(Long id) {
        Opportunity o = oppMapper.selectById(id);
        if (o == null) throw new BizException(40400, "商机不存在或越权");
        return o;
    }

    /* ---------- 新增/编辑 ---------- */

    @Transactional
    public Opportunity create(OpportunityReq req) {
        Customer c = customerMapper.selectById(req.customerId());
        if (c == null) throw new BizException(40400, "关联客户不存在或越权");

        OpportunityStage stage = req.stageId() != null ? requireStage(req.stageId()) : firstStage();
        Opportunity o = new Opportunity();
        o.setTitle(req.title());
        o.setCustomerId(req.customerId());
        o.setStageId(stage.getId());
        o.setStatus(stage.getStageType());
        o.setWinRate(req.winRate() != null ? req.winRate() : stage.getWinRate());
        o.setAmount(req.amount() != null ? req.amount() : BigDecimal.ZERO);
        o.setSource(req.source());
        o.setDemand(req.demand());
        o.setExpectedCloseAt(parse(req.expectedCloseAt()));
        o.setIsPool(0);
        Long owner = req.ownerId() != null ? req.ownerId() : TenantContext.userId();
        o.setOwnerId(owner);
        SysUser u = userMapper.selectById(owner);
        o.setDeptId(u == null ? null : u.getDeptId());
        oppMapper.insert(o);
        return o;
    }

    @Transactional
    public void update(Long id, OpportunityReq req) {
        Opportunity o = get(id);
        o.setTitle(req.title());
        if (req.customerId() != null) o.setCustomerId(req.customerId());
        if (req.amount() != null) o.setAmount(req.amount());
        if (req.winRate() != null) o.setWinRate(req.winRate());
        o.setSource(req.source());
        o.setDemand(req.demand());
        o.setExpectedCloseAt(parse(req.expectedCloseAt()));
        oppMapper.updateById(o);
    }

    @Transactional
    public void delete(Long id) {
        get(id);
        oppMapper.deleteById(id);
    }

    @Transactional
    public void transfer(Long id, Long ownerId) {
        Opportunity o = get(id);
        SysUser u = userMapper.selectById(ownerId);
        if (u == null) throw new BizException(40400, "目标负责人不存在或越权");
        o.setOwnerId(ownerId);
        o.setDeptId(u.getDeptId());
        oppMapper.updateById(o);
    }

    /* ---------- 阶段流转（D1） ---------- */

    @Transactional
    public void moveStage(Long id, Long stageId) {
        Opportunity o = get(id);
        OpportunityStage s = requireStage(stageId);
        if (!"IN_PROGRESS".equals(s.getStageType())) {
            throw new BizException(40920, "终态阶段请用成交/输单接口");
        }
        o.setStageId(s.getId());
        o.setWinRate(s.getWinRate());
        o.setStatus("IN_PROGRESS");
        oppMapper.updateById(o);
    }

    @Transactional
    public void rollback(Long id, Long stageId) {
        // D1：仅销售主管 / 租户管理员可回退
        TenantContext.Principal p = TenantContext.get();
        boolean allowed = p != null && p.roleCodes().stream().anyMatch(ROLLBACK_ROLES::contains);
        if (!allowed) throw new ForbiddenException("仅销售主管/租户管理员可回退阶段");
        Opportunity o = get(id);
        OpportunityStage s = requireStage(stageId);
        if (!"IN_PROGRESS".equals(s.getStageType())) throw new BizException(40920, "只能回退到进行中阶段");
        o.setStageId(s.getId());
        o.setWinRate(s.getWinRate());
        o.setStatus("IN_PROGRESS");
        oppMapper.updateById(o);
    }

    @Transactional
    public void win(Long id, WinReq req) {
        Opportunity o = get(id);
        OpportunityStage won = stageOfType("WON");
        o.setStageId(won.getId());
        o.setStatus("WON");
        o.setWinRate(100);
        o.setDealAmount(req.dealAmount());
        o.setDealAt(parse(req.dealAt()));
        oppMapper.updateById(o);
        writeFollow(id, "成交", "成交金额 " + req.dealAmount() + "；" + req.note());
    }

    @Transactional
    public void lose(Long id, LoseReq req) {
        Opportunity o = get(id);
        OpportunityStage lost = stageOfType("LOST");
        o.setStageId(lost.getId());
        o.setStatus("LOST");
        o.setWinRate(0);
        oppMapper.updateById(o);
        writeFollow(id, "输单",
                "原因：" + req.reason() + "；竞品：" + nz(req.competitor()) + "；复盘：" + nz(req.review()));
    }

    /* ---------- 跟进 ---------- */

    public List<OpportunityFollow> follows(Long oppId) {
        get(oppId);
        return followMapper.selectList(new LambdaQueryWrapper<OpportunityFollow>()
                .eq(OpportunityFollow::getOpportunityId, oppId).orderByDesc(OpportunityFollow::getCreatedAt));
    }

    @Transactional
    public OpportunityFollow addFollow(Long oppId, FollowReq req) {
        Opportunity o = get(oppId);
        OpportunityFollow f = new OpportunityFollow();
        f.setOpportunityId(oppId);
        f.setFollowType(req.followType());
        f.setContent(req.content());
        f.setResult(req.result());
        f.setNextTime(parse(req.nextTime()));
        f.setCreatorId(TenantContext.userId());
        followMapper.insert(f);
        o.setLastFollowAt(LocalDateTime.now());
        if (f.getNextTime() != null) o.setNextFollowAt(f.getNextTime());
        oppMapper.updateById(o);
        return f;
    }

    private void writeFollow(Long oppId, String type, String content) {
        OpportunityFollow f = new OpportunityFollow();
        f.setOpportunityId(oppId);
        f.setFollowType(type);
        f.setContent(content);
        f.setCreatorId(TenantContext.userId());
        followMapper.insert(f);
    }

    private String nz(String s) {
        return s == null ? "-" : s;
    }

    private LocalDateTime parse(String s) {
        if (s == null || s.isBlank()) return null;
        String v = s.trim();
        if (v.length() == 10) v = v + " 00:00:00";
        return LocalDateTime.parse(v, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /* ---------- 跨商机跟进列表（/follow 页） ---------- */

    public IPage<OpportunityFollow> listAllFollows(String keyword, String followType, Long creatorId,
                                                   String start, String end, long page, long size) {
        LambdaQueryWrapper<OpportunityFollow> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) w.like(OpportunityFollow::getContent, keyword);
        if (followType != null && !followType.isBlank()) w.eq(OpportunityFollow::getFollowType, followType);
        if (creatorId != null) w.eq(OpportunityFollow::getCreatorId, creatorId);
        if (start != null && !start.isBlank()) {
            LocalDate sd = LocalDate.parse(start);
            w.ge(OpportunityFollow::getCreatedAt, sd.atStartOfDay());
        }
        if (end != null && !end.isBlank()) {
            LocalDate ed = LocalDate.parse(end);
            w.le(OpportunityFollow::getCreatedAt, ed.atTime(23, 59, 59));
        }
        // 数据范围：只看自己负责的商机的跟进
        Long userId = TenantContext.userId();
        List<Opportunity> myOpps = oppMapper.selectList(new LambdaQueryWrapper<Opportunity>()
                .eq(Opportunity::getOwnerId, userId).select(Opportunity::getId));
        if (myOpps.isEmpty()) {
            w.eq(OpportunityFollow::getId, -1L);
        } else {
            List<Long> oppIds = myOpps.stream().map(Opportunity::getId).toList();
            w.in(OpportunityFollow::getOpportunityId, oppIds);
        }
        w.orderByDesc(OpportunityFollow::getCreatedAt);
        return followMapper.selectPage(new Page<>(page, size), w);
    }

    /* ---------- 详情富化（M08） ---------- */

    public OpportunityDetailVO getDetail(Long id) {
        Opportunity o = get(id);
        String customerName = null;
        String stageName = null;
        String ownerName = null;
        if (o.getCustomerId() != null) {
            Customer c = customerMapper.selectById(o.getCustomerId());
            customerName = c != null ? c.getName() : null;
        }
        if (o.getStageId() != null) {
            OpportunityStage s = stageMapper.selectById(o.getStageId());
            stageName = s != null ? s.getName() : null;
        }
        if (o.getOwnerId() != null) {
            SysUser u = userMapper.selectById(o.getOwnerId());
            ownerName = u != null ? (u.getRealName() != null ? u.getRealName() : u.getUsername()) : null;
        }
        return new OpportunityDetailVO(o, customerName, stageName, ownerName);
    }

    /* ---------- 联系人（代理到客户联系人） ---------- */

    public List<Contact> contacts(Long oppId) {
        Opportunity o = get(oppId);
        return contactMapper.selectList(new LambdaQueryWrapper<Contact>()
                .eq(Contact::getCustomerId, o.getCustomerId()));
    }

    /* ---------- 协作人（M09） ---------- */

    public List<OpportunityCollaborator> collaborators(Long oppId) {
        get(oppId);
        return collabMapper.selectList(new LambdaQueryWrapper<OpportunityCollaborator>()
                .eq(OpportunityCollaborator::getOpportunityId, oppId)
                .orderByDesc(OpportunityCollaborator::getCreatedAt));
    }

    @Transactional
    public OpportunityCollaborator addCollaborator(Long oppId, Long userId, String permissionJson) {
        get(oppId);
        // 检查是否已存在
        OpportunityCollaborator existing = collabMapper.selectOne(
                new LambdaQueryWrapper<OpportunityCollaborator>()
                        .eq(OpportunityCollaborator::getOpportunityId, oppId)
                        .eq(OpportunityCollaborator::getUserId, userId));
        if (existing != null) throw new BizException(40910, "该用户已是协作人");
        OpportunityCollaborator c = new OpportunityCollaborator();
        c.setOpportunityId(oppId);
        c.setUserId(userId);
        c.setPermissionJson(permissionJson);
        c.setStatus("ACTIVE");
        c.setCreatedBy(TenantContext.userId());
        collabMapper.insert(c);
        return c;
    }

    @Transactional
    public void removeCollaborator(Long oppId, Long collabId) {
        get(oppId);
        collabMapper.deleteById(collabId);
    }
}
