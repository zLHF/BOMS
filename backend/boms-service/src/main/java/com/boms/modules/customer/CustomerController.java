package com.boms.modules.customer;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boms.common.audit.AuditLog;
import com.boms.common.result.R;
import com.boms.common.security.RequirePerm;
import com.boms.modules.customer.dto.ContactReq;
import com.boms.modules.customer.dto.CustomerReq;
import com.boms.modules.customer.entity.Contact;
import com.boms.modules.customer.entity.Customer;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 客户 M10。 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @GetMapping
    @RequirePerm("customer:view")
    public R<IPage<Customer>> list(@RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) String level,
                                   @RequestParam(required = false) Long parentId,
                                   @RequestParam(defaultValue = "1") long page,
                                   @RequestParam(defaultValue = "20") long size) {
        return R.ok(service.list(keyword, level, parentId, page, size));
    }

    @GetMapping("/{id}")
    @RequirePerm("customer:view")
    public R<Customer> get(@PathVariable Long id) {
        return R.ok(service.get(id));
    }

    @PostMapping
    @RequirePerm("customer:create")
    @AuditLog(action = "customer:create", objectType = "customer")
    public R<Customer> create(@Valid @RequestBody CustomerReq req) {
        return R.ok(service.create(req));
    }

    @PutMapping("/{id}")
    @RequirePerm("customer:update")
    @AuditLog(action = "customer:update", objectType = "customer")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody CustomerReq req) {
        service.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @RequirePerm("customer:update")
    @AuditLog(action = "customer:delete", objectType = "customer")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    @PostMapping("/{id}/transfer")
    @RequirePerm("customer:transfer")
    @AuditLog(action = "customer:transfer", objectType = "customer")
    public R<Void> transfer(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        service.transfer(id, body.get("ownerId"));
        return R.ok();
    }

    @GetMapping("/{id}/children")
    @RequirePerm("customer:view")
    public R<List<Customer>> children(@PathVariable Long id) {
        return R.ok(service.children(id));
    }

    /* ---------- 联系人 ---------- */

    @GetMapping("/{id}/contacts")
    @RequirePerm("customer:view")
    public R<List<Contact>> contacts(@PathVariable Long id) {
        return R.ok(service.contacts(id));
    }

    @PostMapping("/{id}/contacts")
    @RequirePerm("customer:contact:manage")
    @AuditLog(action = "customer:contact:add", objectType = "contact")
    public R<Contact> addContact(@PathVariable Long id, @Valid @RequestBody ContactReq req) {
        return R.ok(service.addContact(id, req));
    }

    @PutMapping("/{id}/contacts/{cid}")
    @RequirePerm("customer:contact:manage")
    @AuditLog(action = "customer:contact:update", objectType = "contact")
    public R<Void> updateContact(@PathVariable Long id, @PathVariable Long cid, @Valid @RequestBody ContactReq req) {
        service.updateContact(id, cid, req);
        return R.ok();
    }

    @DeleteMapping("/{id}/contacts/{cid}")
    @RequirePerm("customer:contact:manage")
    @AuditLog(action = "customer:contact:delete", objectType = "contact")
    public R<Void> deleteContact(@PathVariable Long id, @PathVariable Long cid) {
        service.deleteContact(id, cid);
        return R.ok();
    }
}
