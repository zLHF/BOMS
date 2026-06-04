package com.boms.modules.audit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boms.common.audit.AuditLogEntity;
import com.boms.common.audit.AuditLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** 操作日志查询服务（写入侧由 @AuditLog AOP 完成，本服务仅负责读取）。 */
@Service
public class AuditLogService {

    private final AuditLogMapper mapper;

    public AuditLogService(AuditLogMapper mapper) {
        this.mapper = mapper;
    }

    /** 分页查询审计日志，租户隔离由 TenantLineInnerInterceptor 自动处理。 */
    public IPage<AuditLogEntity> list(String objectType, Long objectId, Long userId,
                                      String action, String start, String end,
                                      long page, long size) {
        LambdaQueryWrapper<AuditLogEntity> w = new LambdaQueryWrapper<>();
        w.eq(objectType != null, AuditLogEntity::getObjectType, objectType)
         .eq(objectId != null, AuditLogEntity::getObjectId, objectId)
         .eq(userId != null, AuditLogEntity::getUserId, userId)
         .like(action != null && !action.isBlank(), AuditLogEntity::getAction, action)
         .ge(start != null && !start.isBlank(), AuditLogEntity::getCreatedAt, parseDateStart(start))
         .le(end != null && !end.isBlank(), AuditLogEntity::getCreatedAt, parseDateEnd(end))
         .orderByDesc(AuditLogEntity::getCreatedAt);
        return mapper.selectPage(new Page<>(page, size), w);
    }

    private LocalDateTime parseDateStart(String date) {
        try { return LocalDate.parse(date).atStartOfDay(); }
        catch (Exception e) { return null; }
    }

    private LocalDateTime parseDateEnd(String date) {
        try { return LocalDate.parse(date).atTime(LocalTime.MAX); }
        catch (Exception e) { return null; }
    }
}
