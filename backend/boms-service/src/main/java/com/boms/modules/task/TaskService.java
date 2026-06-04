package com.boms.modules.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boms.common.exception.BizException;
import com.boms.common.exception.ForbiddenException;
import com.boms.common.tenant.TenantContext;
import com.boms.modules.system.entity.SysUser;
import com.boms.modules.system.mapper.SysUserMapper;
import com.boms.modules.system.service.ScopeService;
import com.boms.modules.task.dto.TaskAssignReq;
import com.boms.modules.task.dto.TaskCreateReq;
import com.boms.modules.task.dto.TaskUpdateReq;
import com.boms.modules.task.entity.Task;
import com.boms.modules.task.mapper.TaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

/** 任务中心 M12：CRUD + 数据范围 + 状态流转。 */
@Service
public class TaskService {

    private static final Set<String> VALID_STATUS = Set.of("PENDING", "DOING", "DONE", "OVERDUE", "CANCELLED");
    private static final Set<String> ACTIVE_STATUS = Set.of("PENDING", "DOING", "OVERDUE");

    private final TaskMapper taskMapper;
    private final SysUserMapper userMapper;
    private final ScopeService scopeService;

    public TaskService(TaskMapper taskMapper, SysUserMapper userMapper, ScopeService scopeService) {
        this.taskMapper = taskMapper;
        this.userMapper = userMapper;
        this.scopeService = scopeService;
    }

    /* ---------- 列表（视图 + 筛选 + 数据范围） ---------- */

    public IPage<Task> list(String view, String status, String priority, Long assigneeId,
                            String keyword, String objectType, Long objectId,
                            long page, long size) {
        LambdaQueryWrapper<Task> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) w.like(Task::getTitle, keyword);
        if (status != null && !status.isBlank()) w.eq(Task::getStatus, status);
        if (priority != null && !priority.isBlank()) w.eq(Task::getPriority, priority);
        if (assigneeId != null) w.eq(Task::getAssigneeId, assigneeId);
        if (objectType != null && !objectType.isBlank()) w.eq(Task::getObjectType, objectType);
        if (objectId != null) w.eq(Task::getObjectId, objectId);

        // 视图切换（task 表无 dept_id，需通过 assigneeId 关联用户部门）
        String v = view == null ? "mine" : view;
        switch (v) {
            case "mine" -> w.and(ww -> ww
                    .eq(Task::getAssigneeId, TenantContext.userId())
                    .or()
                    .eq(Task::getCreatorId, TenantContext.userId()));
            case "sub" -> {
                if (!TenantContext.hasPerm("task:view:sub")) throw new ForbiddenException("无下属数据查看权限");
                // 查本部门范围内用户 ID 列表
                Set<Long> userIds = scopeService.current().all()
                        ? null
                        : userIdsInScope();
                if (userIds == null) {
                    // 全部可见（TENANT/PLATFORM）
                } else if (userIds.isEmpty()) {
                    w.eq(Task::getId, -1L); // 无可见用户
                } else {
                    w.in(Task::getAssigneeId, userIds);
                }
            }
            default -> {
                // "all" — 使用数据范围
                Set<Long> userIds = scopeService.current().all()
                        ? null
                        : userIdsInScope();
                if (userIds != null && userIds.isEmpty()) {
                    w.eq(Task::getId, -1L);
                } else if (userIds != null) {
                    w.and(ww -> ww.in(Task::getAssigneeId, userIds)
                            .or().eq(Task::getCreatorId, TenantContext.userId()));
                }
            }
        }

        w.orderByDesc(Task::getUpdatedAt);
        return taskMapper.selectPage(new Page<>(page, size), w);
    }

    /** 获取当前用户数据范围内的用户 ID（用于 task 表无 dept_id 的场景）。 */
    private Set<Long> userIdsInScope() {
        var sf = scopeService.current();
        if (sf.all()) return null;
        if (sf.selfOnly()) return Set.of(TenantContext.userId());
        // 查询部门内的用户
        var userQuery = new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getDeptId, sf.deptIds())
                .select(SysUser::getId);
        List<SysUser> users = userMapper.selectList(userQuery);
        Set<Long> ids = new java.util.HashSet<>();
        for (SysUser u : users) ids.add(u.getId());
        ids.add(TenantContext.userId()); // 始终包含自己
        return ids;
    }

    public Task get(Long id) {
        Task t = taskMapper.selectById(id);
        if (t == null) throw new BizException(40400, "任务不存在或越权");
        return t;
    }

    /* ---------- 创建 ---------- */

    @Transactional
    public Task create(TaskCreateReq req) {
        Task t = new Task();
        t.setTitle(req.title());
        t.setContent(req.content());
        t.setObjectType(req.objectType());
        t.setObjectId(req.objectId());
        t.setAssigneeId(req.assigneeId() != null ? req.assigneeId() : TenantContext.userId());
        t.setCreatorId(TenantContext.userId());
        t.setDueAt(parse(req.dueAt()));
        t.setPriority(req.priority() != null ? req.priority() : "NORMAL");
        t.setStatus("PENDING");
        // 验证 assignee 存在
        if (req.assigneeId() != null) {
            SysUser u = userMapper.selectById(req.assigneeId());
            if (u == null) throw new BizException(40400, "指派用户不存在或越权");
        }
        taskMapper.insert(t);
        return t;
    }

    /* ---------- 更新 ---------- */

    @Transactional
    public void update(Long id, TaskUpdateReq req) {
        Task t = get(id);
        if (req.title() != null) t.setTitle(req.title());
        if (req.content() != null) t.setContent(req.content());
        if (req.priority() != null) t.setPriority(req.priority());
        if (req.dueAt() != null) t.setDueAt(parse(req.dueAt()));
        taskMapper.updateById(t);
    }

    /* ---------- 指派 ---------- */

    @Transactional
    public void assign(Long id, Long assigneeId) {
        Task t = get(id);
        SysUser u = userMapper.selectById(assigneeId);
        if (u == null) throw new BizException(40400, "指派用户不存在或越权");
        t.setAssigneeId(assigneeId);
        taskMapper.updateById(t);
    }

    /* ---------- 取消 ---------- */

    @Transactional
    public void cancel(Long id) {
        Task t = get(id);
        if (!ACTIVE_STATUS.contains(t.getStatus())) {
            throw new BizException(40920, "当前状态不可取消");
        }
        t.setStatus("CANCELLED");
        taskMapper.updateById(t);
    }

    /* ---------- 状态变更（开始/完成） ---------- */

    @Transactional
    public void changeStatus(Long id, String newStatus) {
        if (!VALID_STATUS.contains(newStatus)) throw new BizException(40000, "无效状态：" + newStatus);
        Task t = get(id);
        String cur = t.getStatus();
        // 合法流转：PENDING→DOING, DOING→DONE, *→CANCELLED
        boolean valid = switch (newStatus) {
            case "DOING" -> "PENDING".equals(cur) || "OVERDUE".equals(cur);
            case "DONE" -> "DOING".equals(cur) || "OVERDUE".equals(cur);
            case "CANCELLED" -> ACTIVE_STATUS.contains(cur);
            default -> false;
        };
        if (!valid) throw new BizException(40920, "不允许从 " + cur + " 变更为 " + newStatus);
        t.setStatus(newStatus);
        taskMapper.updateById(t);
    }

    private LocalDateTime parse(String s) {
        if (s == null || s.isBlank()) return null;
        String v = s.trim();
        if (v.length() == 10) v = v + " 00:00:00";
        return LocalDateTime.parse(v, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
