package com.boms.modules.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 附件实体（对应 attachment 表，多态关联 opportunity/customer 等）。 */
@Data
@TableName("attachment")
public class Attachment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String objectType;
    private Long objectId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String contentType;
    /** PENDING=待确认上传, CONFIRMED=已确认。 */
    private String status;
    private Integer version;
    private Long uploaderId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
