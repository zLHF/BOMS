package com.boms.modules.customer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boms.common.exception.BizException;
import com.boms.common.tenant.TenantContext;
import com.boms.modules.customer.dto.ContactReq;
import com.boms.modules.customer.dto.CustomerReq;
import com.boms.modules.customer.entity.Contact;
import com.boms.modules.customer.entity.Customer;
import com.boms.modules.customer.mapper.ContactMapper;
import com.boms.modules.customer.mapper.CustomerMapper;
import com.boms.modules.system.entity.SysUser;
import com.boms.modules.system.mapper.SysUserMapper;
import com.boms.modules.system.service.ScopeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 客户管理 M10：CRUD + 列表（数据范围）+ 联系人 + 下级客户。 */
@Service
public class CustomerService {

    private final CustomerMapper customerMapper;
    private final ContactMapper contactMapper;
    private final SysUserMapper userMapper;
    private final ScopeService scopeService;

    public CustomerService(CustomerMapper customerMapper, ContactMapper contactMapper,
                           SysUserMapper userMapper, ScopeService scopeService) {
        this.customerMapper = customerMapper;
        this.contactMapper = contactMapper;
        this.userMapper = userMapper;
        this.scopeService = scopeService;
    }

    public IPage<Customer> list(String keyword, String level, Long parentId, long page, long size) {
        LambdaQueryWrapper<Customer> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) w.like(Customer::getName, keyword);
        if (level != null && !level.isBlank()) w.eq(Customer::getLevel, level);
        if (parentId != null) w.eq(Customer::getParentId, parentId);
        scopeService.apply(w, Customer::getOwnerId, Customer::getDeptId);
        w.orderByDesc(Customer::getUpdatedAt);
        return customerMapper.selectPage(new Page<>(page, size), w);
    }

    public Customer get(Long id) {
        Customer c = customerMapper.selectById(id);
        if (c == null) throw new BizException(40400, "客户不存在或越权");
        return c;
    }

    @Transactional
    public Customer create(CustomerReq req) {
        String credit = req.creditCode() == null || req.creditCode().isBlank() ? null : req.creditCode().trim();
        // D2：信用代码填了才租户内唯一
        if (credit != null) {
            long dup = customerMapper.selectCount(new LambdaQueryWrapper<Customer>().eq(Customer::getCreditCode, credit));
            if (dup > 0) throw new BizException(40911, "统一社会信用代码已存在");
        }
        // D2：名称不唯一，疑似重复软提示（force=true 跳过）
        if (!Boolean.TRUE.equals(req.force())) {
            long nameDup = customerMapper.selectCount(new LambdaQueryWrapper<Customer>().eq(Customer::getName, req.name()));
            if (nameDup > 0) throw new BizException(40910, "疑似重复客户：「" + req.name() + "」已存在，确认继续请重试");
        }
        Customer c = new Customer();
        c.setName(req.name());
        c.setCreditCode(credit);
        c.setType(req.type());
        c.setIndustry(req.industry());
        c.setRegion(req.region());
        c.setLevel(req.level());
        c.setParentId(req.parentId() == null ? 0L : req.parentId());
        c.setStatus("ACTIVE");
        c.setIsPool(0);
        applyOwner(c, req.ownerId(), req.deptId());
        customerMapper.insert(c);
        return c;
    }

    @Transactional
    public void update(Long id, CustomerReq req) {
        Customer c = get(id);
        if (req.name() != null) c.setName(req.name());
        String credit = req.creditCode() == null || req.creditCode().isBlank() ? null : req.creditCode().trim();
        if (credit != null && !credit.equals(c.getCreditCode())) {
            long dup = customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                    .eq(Customer::getCreditCode, credit).ne(Customer::getId, id));
            if (dup > 0) throw new BizException(40911, "统一社会信用代码已存在");
        }
        c.setCreditCode(credit);
        c.setType(req.type());
        c.setIndustry(req.industry());
        c.setRegion(req.region());
        c.setLevel(req.level());
        if (req.parentId() != null) c.setParentId(req.parentId());
        customerMapper.updateById(c);
    }

    @Transactional
    public void delete(Long id) {
        get(id);
        long children = customerMapper.selectCount(new LambdaQueryWrapper<Customer>().eq(Customer::getParentId, id));
        if (children > 0) throw new BizException(40902, "存在下级客户，无法删除");
        customerMapper.deleteById(id);
    }

    @Transactional
    public void transfer(Long id, Long newOwnerId) {
        Customer c = get(id);
        SysUser u = userMapper.selectById(newOwnerId);
        if (u == null) throw new BizException(40400, "目标负责人不存在或越权");
        c.setOwnerId(newOwnerId);
        c.setDeptId(u.getDeptId());
        customerMapper.updateById(c);
    }

    public List<Customer> children(Long parentId) {
        get(parentId);
        return customerMapper.selectList(new LambdaQueryWrapper<Customer>().eq(Customer::getParentId, parentId));
    }

    /* ---------- 联系人 ---------- */

    public List<Contact> contacts(Long customerId) {
        get(customerId);
        return contactMapper.selectList(new LambdaQueryWrapper<Contact>()
                .eq(Contact::getCustomerId, customerId).orderByDesc(Contact::getIsKeyPerson));
    }

    @Transactional
    public Contact addContact(Long customerId, ContactReq req) {
        get(customerId);
        Contact ct = new Contact();
        ct.setCustomerId(customerId);
        ct.setName(req.name());
        ct.setTitle(req.title());
        ct.setMobile(req.mobile());
        ct.setEmail(req.email());
        ct.setIsKeyPerson(Boolean.TRUE.equals(req.keyPerson()) ? 1 : 0);
        contactMapper.insert(ct);
        return ct;
    }

    @Transactional
    public void updateContact(Long customerId, Long contactId, ContactReq req) {
        get(customerId);
        Contact ct = contactMapper.selectById(contactId);
        if (ct == null || !ct.getCustomerId().equals(customerId)) throw new BizException(40400, "联系人不存在");
        ct.setName(req.name());
        ct.setTitle(req.title());
        ct.setMobile(req.mobile());
        ct.setEmail(req.email());
        ct.setIsKeyPerson(Boolean.TRUE.equals(req.keyPerson()) ? 1 : 0);
        contactMapper.updateById(ct);
    }

    @Transactional
    public void deleteContact(Long customerId, Long contactId) {
        get(customerId);
        contactMapper.deleteById(contactId);
    }

    /** 负责人默认当前用户，部门取负责人部门（数据范围依赖 dept_id 冗余）。 */
    private void applyOwner(Customer c, Long ownerId, Long deptId) {
        Long owner = ownerId != null ? ownerId : TenantContext.userId();
        c.setOwnerId(owner);
        if (deptId != null) {
            c.setDeptId(deptId);
        } else {
            SysUser u = userMapper.selectById(owner);
            c.setDeptId(u == null ? null : u.getDeptId());
        }
    }
}
