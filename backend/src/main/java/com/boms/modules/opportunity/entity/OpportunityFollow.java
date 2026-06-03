package com.boms.modules.opportunity.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("opportunity_follow")
public class OpportunityFollow {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long opportunityId;
    private String followType;
    private String content;
    private String result;
    private LocalDateTime nextTime;
    private Long creatorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
