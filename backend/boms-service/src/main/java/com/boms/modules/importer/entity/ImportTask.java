package com.boms.modules.importer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 导入任务实体（对应 import_task 表）。 */
@Data
@TableName("import_task")
public class ImportTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String bizType;
    private String fileName;
    private String status;     // RUNNING / DONE / FAILED
    private Integer total;
    private Integer success;
    private Integer failed;
    private String errorFilePath;
    private Long operatorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
