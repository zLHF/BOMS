package com.boms.modules.system;

import com.boms.common.audit.AuditLog;
import com.boms.common.result.R;
import com.boms.common.security.RequirePerm;
import com.boms.modules.system.entity.Department;
import com.boms.modules.system.service.DeptService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 组织 - 部门树 M03。 */
@RestController
@RequestMapping("/api/depts")
public class DeptController {

    private final DeptService deptService;

    public DeptController(DeptService deptService) {
        this.deptService = deptService;
    }

    @GetMapping
    @RequirePerm("org:dept:view")
    public R<List<Map<String, Object>>> tree() {
        return R.ok(deptService.tree());
    }

    @PostMapping
    @RequirePerm("org:dept:manage")
    @AuditLog(action = "org:dept:create", objectType = "department")
    public R<Department> create(@RequestBody Department dept) {
        return R.ok(deptService.create(dept));
    }

    @PutMapping("/{id}")
    @RequirePerm("org:dept:manage")
    @AuditLog(action = "org:dept:update", objectType = "department")
    public R<Void> update(@PathVariable Long id, @RequestBody Department dept) {
        deptService.update(id, dept);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @RequirePerm("org:dept:manage")
    @AuditLog(action = "org:dept:delete", objectType = "department")
    public R<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return R.ok();
    }

    @PutMapping("/sort")
    @RequirePerm("org:dept:manage")
    @AuditLog(action = "org:dept:sort", objectType = "department")
    public R<Void> sort(@RequestBody List<Map<String, Object>> items) {
        deptService.sort(items);
        return R.ok();
    }
}
