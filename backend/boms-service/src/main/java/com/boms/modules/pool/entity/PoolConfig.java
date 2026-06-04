package com.boms.modules.pool.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 公海池配置（每租户一行）。 */
@Data
@TableName("pool_config")
public class PoolConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Integer autoRecycleEnabled;
    private Integer noFollowDays;
    private Integer protectionDays;
    private Integer personalLimit;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
