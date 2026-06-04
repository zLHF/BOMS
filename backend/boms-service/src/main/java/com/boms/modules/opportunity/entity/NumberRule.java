package com.boms.modules.opportunity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 编号规则（M19-03），映射 number_rule 表。 */
@Data
@TableName("number_rule")
public class NumberRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String bizType;     // opportunity / customer
    private String prefix;      // 如 OPP
    private String dateFormat;  // 如 yyyyMMdd
    private Integer seqLength;
    private Long currentSeq;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
