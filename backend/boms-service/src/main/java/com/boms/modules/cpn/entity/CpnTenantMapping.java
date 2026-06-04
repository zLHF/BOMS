package com.boms.modules.cpn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 算力平台租户映射。 */
@Data
@TableName("cpn_tenant_mapping")
public class CpnTenantMapping {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long bomsTenantId;
    private String cpnEnterpriseName;
    private String cpnTenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
