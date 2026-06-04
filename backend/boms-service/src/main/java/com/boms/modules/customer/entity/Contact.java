package com.boms.modules.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("contact")
public class Contact {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long customerId;
    private String name;
    private String title;
    private String mobile;
    private String email;
    private Integer isKeyPerson;

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
