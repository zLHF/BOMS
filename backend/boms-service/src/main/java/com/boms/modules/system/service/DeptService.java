package com.boms.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boms.common.exception.BizException;
import com.boms.modules.system.entity.Department;
import com.boms.modules.system.entity.SysUser;
import com.boms.modules.system.mapper.DepartmentMapper;
import com.boms.modules.system.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/** 部门树管理 + 数据范围用的子树计算（M03）。 */
@Service
public class DeptService {

    private final DepartmentMapper deptMapper;
    private final SysUserMapper userMapper;

    public DeptService(DepartmentMapper deptMapper, SysUserMapper userMapper) {
        this.deptMapper = deptMapper;
        this.userMapper = userMapper;
    }

    public List<Department> listAll() {
        return deptMapper.selectList(new LambdaQueryWrapper<Department>()
                .orderByAsc(Department::getSort).orderByAsc(Department::getId));
    }

    /** 组装为树（parent_id=0 为根）。 */
    public List<Map<String, Object>> tree() {
        List<Department> all = listAll();
        Map<Long, Map<String, Object>> nodes = new LinkedHashMap<>();
        for (Department d : all) {
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("id", d.getId());
            n.put("parentId", d.getParentId());
            n.put("name", d.getName());
            n.put("leaderId", d.getLeaderId());
            n.put("sort", d.getSort());
            n.put("status", d.getStatus());
            n.put("children", new ArrayList<>());
            nodes.put(d.getId(), n);
        }
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Department d : all) {
            Map<String, Object> n = nodes.get(d.getId());
            Map<String, Object> parent = nodes.get(d.getParentId());
            if (parent != null) {
                ((List<Object>) parent.get("children")).add(n);
            } else {
                roots.add(n);
            }
        }
        return roots;
    }

    /** 某部门的自身 + 全部子孙部门 ID（数据范围 DEPT_AND_SUB 用）。 */
    public Set<Long> descendantIds(Long rootDeptId) {
        if (rootDeptId == null) return Set.of();
        Map<Long, List<Long>> childrenOf = new HashMap<>();
        for (Department d : listAll()) {
            childrenOf.computeIfAbsent(d.getParentId(), k -> new ArrayList<>()).add(d.getId());
        }
        Set<Long> result = new HashSet<>();
        Deque<Long> stack = new ArrayDeque<>();
        stack.push(rootDeptId);
        while (!stack.isEmpty()) {
            Long cur = stack.pop();
            if (result.add(cur)) {
                childrenOf.getOrDefault(cur, List.of()).forEach(stack::push);
            }
        }
        return result;
    }

    @Transactional
    public Department create(Department d) {
        d.setId(null);
        if (d.getParentId() == null) d.setParentId(0L);
        if (d.getSort() == null) d.setSort(0);
        if (d.getStatus() == null) d.setStatus("ENABLED");
        deptMapper.insert(d);
        return d;
    }

    @Transactional
    public void update(Long id, Department patch) {
        Department exist = deptMapper.selectById(id);
        if (exist == null) throw new BizException(40400, "部门不存在");
        if (patch.getName() != null) exist.setName(patch.getName());
        if (patch.getParentId() != null) {
            if (patch.getParentId().equals(id)) throw new BizException(40901, "父部门不能为自身");
            if (descendantIds(id).contains(patch.getParentId())) {
                throw new BizException(40901, "父部门不能是自身的子部门");
            }
            exist.setParentId(patch.getParentId());
        }
        if (patch.getLeaderId() != null) exist.setLeaderId(patch.getLeaderId());
        if (patch.getSort() != null) exist.setSort(patch.getSort());
        if (patch.getStatus() != null) exist.setStatus(patch.getStatus());
        deptMapper.updateById(exist);
    }

    @Transactional
    public void delete(Long id) {
        Set<Long> subtree = descendantIds(id);
        long childCount = subtree.size() - 1;
        if (childCount > 0) throw new BizException(40902, "请先删除子部门");
        long userCount = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeptId, id));
        if (userCount > 0) throw new BizException(40903, "部门下存在用户，无法删除");
        deptMapper.deleteById(id);
    }

    @Transactional
    public void sort(List<Map<String, Object>> items) {
        for (Map<String, Object> it : items) {
            Long id = Long.valueOf(String.valueOf(it.get("id")));
            Department d = deptMapper.selectById(id);
            if (d == null) continue;
            if (it.get("sort") != null) d.setSort(Integer.valueOf(String.valueOf(it.get("sort"))));
            if (it.get("parentId") != null) d.setParentId(Long.valueOf(String.valueOf(it.get("parentId"))));
            deptMapper.updateById(d);
        }
    }

    public List<Long> idsByName(String namePrefix) {
        return listAll().stream()
                .filter(d -> namePrefix == null || d.getName().contains(namePrefix))
                .map(Department::getId).collect(Collectors.toList());
    }
}
