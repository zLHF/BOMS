package com.boms.modules.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("customer")
public class Customer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String code;
    private String name;
    private String creditCode;
    private String type;
    private String industry;
    private String region;
    private String level;       // A/B/C
    private Long ownerId;
    private Long deptId;
    private Long parentId;
    private String status;      // ACTIVE/INACTIVE
    private Integer isPool;
    private LocalDateTime poolRecycledAt;
    private String poolReason;

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
    @Version
    private Integer version;
}
