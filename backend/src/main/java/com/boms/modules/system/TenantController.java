package com.boms.modules.system;

import com.boms.common.audit.AuditLog;
import com.boms.common.result.R;
import com.boms.common.security.RequirePerm;
import com.boms.modules.system.dto.PackageReq;
import com.boms.modules.system.dto.StatusReq;
import com.boms.modules.system.dto.TenantCreateReq;
import com.boms.modules.system.dto.TenantUpdateReq;
import com.boms.modules.system.entity.SysPackage;
import com.boms.modules.system.entity.Tenant;
import com.boms.modules.system.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 租户管理 M02（平台级，不带 tenant_id）。 */
@RestController
@RequestMapping("/api/platform")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping("/tenants")
    @RequirePerm("tenant:view")
    public R<List<Tenant>> list() {
        return R.ok(tenantService.list());
    }

    @GetMapping("/packages")
    @RequirePerm("tenant:view")
    public R<List<SysPackage>> packages() {
        return R.ok(tenantService.packages());
    }

    @PostMapping("/tenants")
    @RequirePerm("tenant:create")
    @AuditLog(action = "tenant:create", objectType = "tenant")
    public R<Map<String, Object>> create(@Valid @RequestBody TenantCreateReq req) {
        return R.ok(tenantService.create(req));
    }

    @PutMapping("/tenants/{id}")
    @RequirePerm("tenant:update")
    @AuditLog(action = "tenant:update", objectType = "tenant")
    public R<Void> update(@PathVariable Long id, @RequestBody TenantUpdateReq req) {
        tenantService.update(id, req);
        return R.ok();
    }

    @PatchMapping("/tenants/{id}/status")
    @RequirePerm("tenant:status")
    @AuditLog(action = "tenant:status", objectType = "tenant")
    public R<Void> status(@PathVariable Long id, @Valid @RequestBody StatusReq req) {
        tenantService.changeStatus(id, req.status());
        return R.ok();
    }

    @PutMapping("/tenants/{id}/package")
    @RequirePerm("tenant:package")
    @AuditLog(action = "tenant:package", objectType = "tenant")
    public R<Void> changePackage(@PathVariable Long id, @Valid @RequestBody PackageReq req) {
        tenantService.changePackage(id, req.packageId());
        return R.ok();
    }
}
