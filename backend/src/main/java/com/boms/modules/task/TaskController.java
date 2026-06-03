package com.boms.modules.task;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boms.common.audit.AuditLog;
import com.boms.common.result.R;
import com.boms.common.security.RequirePerm;
import com.boms.modules.task.dto.TaskAssignReq;
import com.boms.modules.task.dto.TaskCreateReq;
import com.boms.modules.task.dto.TaskUpdateReq;
import com.boms.modules.task.entity.Task;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** 任务中心 M12。 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping
    @RequirePerm("task:view")
    public R<IPage<Task>> list(
            @RequestParam(defaultValue = "mine") String view,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String objectType,
            @RequestParam(required = false) Long objectId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return R.ok(service.list(view, status, priority, assigneeId, keyword, objectType, objectId, page, size));
    }

    @PostMapping
    @RequirePerm("task:create")
    @AuditLog(action = "task:create", objectType = "task")
    public R<Task> create(@Valid @RequestBody TaskCreateReq req) {
        return R.ok(service.create(req));
    }

    @PutMapping("/{id}")
    @RequirePerm("task:update")
    @AuditLog(action = "task:update", objectType = "task")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody TaskUpdateReq req) {
        service.update(id, req);
        return R.ok();
    }

    @PostMapping("/{id}/assign")
    @RequirePerm("task:assign")
    @AuditLog(action = "task:assign", objectType = "task")
    public R<Void> assign(@PathVariable Long id, @Valid @RequestBody TaskAssignReq req) {
        service.assign(id, req.assigneeId());
        return R.ok();
    }

    @PostMapping("/{id}/cancel")
    @RequirePerm("task:cancel")
    @AuditLog(action = "task:cancel", objectType = "task")
    public R<Void> cancel(@PathVariable Long id) {
        service.cancel(id);
        return R.ok();
    }

    @PostMapping("/{id}/status")
    @RequirePerm("task:update")
    @AuditLog(action = "task:status", objectType = "task")
    public R<Void> changeStatus(@PathVariable Long id, @RequestParam String status) {
        service.changeStatus(id, status);
        return R.ok();
    }
}
