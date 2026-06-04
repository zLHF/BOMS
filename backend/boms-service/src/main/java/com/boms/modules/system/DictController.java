package com.boms.modules.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boms.common.audit.AuditLog;
import com.boms.common.result.R;
import com.boms.common.security.RequirePerm;
import com.boms.modules.system.entity.SysDict;
import com.boms.modules.system.service.DictService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 数据字典管理。 */
@RestController
@RequestMapping("/api/dicts")
public class DictController {

    private final DictService service;

    public DictController(DictService service) {
        this.service = service;
    }

    /** 查某类字典项（登录态即可，用于表单下拉）。 */
    @GetMapping("/{dictType}")
    public R<List<SysDict>> listByType(@PathVariable String dictType) {
        return R.ok(service.listByType(dictType));
    }

    /** 查所有字典类型列表。 */
    @GetMapping("/types")
    public R<List<String>> listTypes() {
        return R.ok(service.listTypes());
    }

    /** 管理端分页查询。 */
    @GetMapping
    @RequirePerm("config:dict")
    public R<IPage<SysDict>> listPage(
            @RequestParam(required = false) String dictType,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return R.ok(service.listPage(dictType, page, size));
    }

    /** 新增字典项。 */
    @PostMapping
    @RequirePerm("config:dict")
    @AuditLog(action = "dict:create", objectType = "sys_dict")
    public R<SysDict> create(@RequestBody SysDict body) {
        return R.ok(service.create(body));
    }

    /** 更新字典项。 */
    @PutMapping("/{id}")
    @RequirePerm("config:dict")
    @AuditLog(action = "dict:update", objectType = "sys_dict")
    public R<Void> update(@PathVariable Long id, @RequestBody SysDict body) {
        service.update(id, body);
        return R.ok();
    }

    /** 删除字典项。 */
    @DeleteMapping("/{id}")
    @RequirePerm("config:dict")
    @AuditLog(action = "dict:delete", objectType = "sys_dict")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }
}
