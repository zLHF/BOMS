package com.boms.common.audit;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 对应 audit_log 表（无逻辑删除/乐观锁）。 */
@Data
@TableName("audit_log")
public class AuditLogEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long userId;
    private String userName;
    private String objectType;
    private Long objectId;
    private String action;
    private String beforeJson;
    private String afterJson;
    private String result;
    private String ip;
    private String userAgent;
    private LocalDateTime createdAt;
}
