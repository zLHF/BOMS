package com.boms.modules.cpn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 算力平台用户映射。 */
@Data
@TableName("cpn_user_mapping")
public class CpnUserMapping {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long bomsUserId;
    private String cpnLoginName;
    private String cpnUserCn;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
