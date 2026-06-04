package com.boms.modules.opportunity.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("opportunity")
public class Opportunity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String code;
    private String title;
    private Long customerId;
    private Long stageId;
    private String source;
    private BigDecimal amount;
    private Integer winRate;
    private Long ownerId;
    private Long deptId;
    private String status;          // IN_PROGRESS/WON/LOST/VOID
    private String demand;
    private LocalDateTime expectedCloseAt;
    private LocalDateTime lastFollowAt;
    private LocalDateTime nextFollowAt;
    private BigDecimal dealAmount;
    private LocalDateTime dealAt;
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
