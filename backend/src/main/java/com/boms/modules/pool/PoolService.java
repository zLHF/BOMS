package com.boms.modules.pool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boms.common.exception.BizException;
import com.boms.common.tenant.TenantContext;
import com.boms.modules.customer.entity.Customer;
import com.boms.modules.customer.mapper.CustomerMapper;
import com.boms.modules.opportunity.entity.Opportunity;
import com.boms.modules.opportunity.mapper.OpportunityMapper;
import com.boms.modules.system.entity.SysUser;
import com.boms.modules.system.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 公海池服务 D5（V1.0 手动回收/认领，无自动定时任务）。 */
@Service
public class PoolService {

    private final OpportunityMapper oppMapper;
    private final CustomerMapper customerMapper;
    private final SysUserMapper userMapper;

    public PoolService(OpportunityMapper oppMapper, CustomerMapper customerMapper, SysUserMapper userMapper) {
        this.oppMapper = oppMapper;
        this.customerMapper = customerMapper;
        this.userMapper = userMapper;
    }

    /* ---------- 商机公海 ---------- */

    public IPage<Opportunity> listPoolOpportunities(String keyword, Long stageId, long page, long size) {
        LambdaQueryWrapper<Opportunity> w = new LambdaQueryWrapper<>();
        w.eq(Opportunity::getIsPool, 1);
        if (keyword != null && !keyword.isBlank()) w.like(Opportunity::getTitle, keyword);
        if (stageId != null) w.eq(Opportunity::getStageId, stageId);
        w.orderByDesc(Opportunity::getPoolRecycledAt);
        return oppMapper.selectPage(new Page<>(page, size), w);
    }

    @Transactional
    public void claimOpportunity(Long id) {
        Opportunity o = oppMapper.selectById(id);
        if (o == null) throw new BizException(40400, "商机不存在");
        if (o.getIsPool() == null || o.getIsPool() != 1) throw new BizException(40920, "该商机不在公海中");
        Long userId = TenantContext.userId();
        SysUser u = userMapper.selectById(userId);
        o.setOwnerId(userId);
        o.setDeptId(u != null ? u.getDeptId() : null);
        o.setIsPool(0);
        o.setPoolReason(null);
        o.setPoolRecycledAt(null);
        oppMapper.updateById(o);
    }

    @Transactional
    public void recycleOpportunity(Long id, String reason) {
        Opportunity o = oppMapper.selectById(id);
        if (o == null) throw new BizException(40400, "商机不存在");
        if (o.getIsPool() != null && o.getIsPool() == 1) throw new BizException(40920, "该商机已在公海中");
        o.setIsPool(1);
        o.setPoolRecycledAt(LocalDateTime.now());
        o.setPoolReason(reason);
        o.setOwnerId(null);
        o.setDeptId(null);
        oppMapper.updateById(o);
    }

    /* ---------- 客户公海 ---------- */

    public IPage<Customer> listPoolCustomers(String keyword, String level, long page, long size) {
        LambdaQueryWrapper<Customer> w = new LambdaQueryWrapper<>();
        w.eq(Customer::getIsPool, 1);
        if (keyword != null && !keyword.isBlank()) w.like(Customer::getName, keyword);
        if (level != null && !level.isBlank()) w.eq(Customer::getLevel, level);
        w.orderByDesc(Customer::getPoolRecycledAt);
        return customerMapper.selectPage(new Page<>(page, size), w);
    }

    @Transactional
    public void claimCustomer(Long id) {
        Customer c = customerMapper.selectById(id);
        if (c == null) throw new BizException(40400, "客户不存在");
        if (c.getIsPool() == null || c.getIsPool() != 1) throw new BizException(40920, "该客户不在公海中");
        Long userId = TenantContext.userId();
        SysUser u = userMapper.selectById(userId);
        c.setOwnerId(userId);
        c.setIsPool(0);
        c.setPoolReason(null);
        c.setPoolRecycledAt(null);
        customerMapper.updateById(c);
    }

    @Transactional
    public void recycleCustomer(Long id, String reason) {
        Customer c = customerMapper.selectById(id);
        if (c == null) throw new BizException(40400, "客户不存在");
        if (c.getIsPool() != null && c.getIsPool() == 1) throw new BizException(40920, "该客户已在公海中");
        c.setIsPool(1);
        c.setPoolRecycledAt(LocalDateTime.now());
        c.setPoolReason(reason);
        c.setOwnerId(null);
        customerMapper.updateById(c);
    }
}
