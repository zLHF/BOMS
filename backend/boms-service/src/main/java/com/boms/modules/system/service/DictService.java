package com.boms.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boms.common.exception.BizException;
import com.boms.common.tenant.TenantContext;
import com.boms.modules.system.entity.SysDict;
import com.boms.modules.system.mapper.SysDictMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 数据字典服务。 */
@Service
public class DictService {

    private final SysDictMapper dictMapper;

    public DictService(SysDictMapper dictMapper) {
        this.dictMapper = dictMapper;
    }

    /** 查某类字典项（登录态即可，用于下拉）。 */
    public List<SysDict> listByType(String dictType) {
        Long tid = TenantContext.tenantId();
        return dictMapper.selectList(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getTenantId, tid)
                .eq(SysDict::getDictType, dictType)
                .eq(SysDict::getIsActive, 1)
                .orderByAsc(SysDict::getSort));
    }

    /** 查所有字典类型列表（去重）。 */
    public List<String> listTypes() {
        Long tid = TenantContext.tenantId();
        List<SysDict> all = dictMapper.selectList(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getTenantId, tid)
                .select(SysDict::getDictType)
                .groupBy(SysDict::getDictType));
        return all.stream().map(SysDict::getDictType).distinct().toList();
    }

    /** 管理端分页查询。 */
    public IPage<SysDict> listPage(String dictType, long page, long size) {
        Long tid = TenantContext.tenantId();
        LambdaQueryWrapper<SysDict> w = new LambdaQueryWrapper<>();
        w.eq(SysDict::getTenantId, tid);
        if (dictType != null && !dictType.isBlank()) w.eq(SysDict::getDictType, dictType);
        w.orderByAsc(SysDict::getDictType).orderByAsc(SysDict::getSort);
        return dictMapper.selectPage(new Page<>(page, size), w);
    }

    @Transactional
    public SysDict create(SysDict body) {
        Long tid = TenantContext.tenantId();
        body.setTenantId(tid);
        body.setIsActive(1);
        dictMapper.insert(body);
        return body;
    }

    @Transactional
    public void update(Long id, SysDict body) {
        SysDict d = dictMapper.selectById(id);
        if (d == null || !d.getTenantId().equals(TenantContext.tenantId())) {
            throw new BizException(40400, "字典项不存在");
        }
        if (body.getDictType() != null) d.setDictType(body.getDictType());
        if (body.getItemCode() != null) d.setItemCode(body.getItemCode());
        if (body.getItemLabel() != null) d.setItemLabel(body.getItemLabel());
        if (body.getSort() != null) d.setSort(body.getSort());
        if (body.getIsActive() != null) d.setIsActive(body.getIsActive());
        dictMapper.updateById(d);
    }

    @Transactional
    public void delete(Long id) {
        SysDict d = dictMapper.selectById(id);
        if (d == null || !d.getTenantId().equals(TenantContext.tenantId())) {
            throw new BizException(40400, "字典项不存在");
        }
        dictMapper.deleteById(id);
    }
}
