package com.boms.modules.audit;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boms.common.audit.AuditLogEntity;
import com.boms.common.result.R;
import com.boms.common.security.RequirePerm;
import org.springframework.web.bind.annotation.*;

/** 操作日志 M16（只读查询，写入由 @AuditLog AOP 拦截）。 */
@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService service;

    public AuditLogController(AuditLogService service) {
        this.service = service;
    }

    @GetMapping
    @RequirePerm("audit:view")
    public R<IPage<AuditLogEntity>> list(
            @RequestParam(required = false) String objectType,
            @RequestParam(required = false) Long objectId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return R.ok(service.list(objectType, objectId, userId, action, start, end, page, size));
    }
}
