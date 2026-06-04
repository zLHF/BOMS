package com.boms.modules.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 平台级租户表（无 tenant_id 行级过滤）。 */
@Data
@TableName("tenant")
public class Tenant {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String code;
    private String domain;
    private String status;       // TRIAL/NORMAL/EXPIRING/FROZEN/DISABLED
    private Long packageId;
    private LocalDateTime expireAt;
    private String configJson;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
