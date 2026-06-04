package com.boms.modules.task.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 任务（M12），多态关联 opportunity/customer。 */
@Data
@TableName("task")
public class Task {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String objectType;
    private Long objectId;
    private String title;
    private String content;
    private Long assigneeId;
    private Long creatorId;
    private LocalDateTime dueAt;
    private String priority;    // HIGH / NORMAL / LOW
    private String status;      // PENDING / DOING / DONE / OVERDUE / CANCELLED

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
