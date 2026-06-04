package com.boms.modules.opportunity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("opportunity_stage")
public class OpportunityStage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String code;        // prospecting/qualifying/proposal/negotiation/won/lost/void
    private String name;
    private Integer sort;
    private Integer winRate;
    private String color;
    private String stageType;   // IN_PROGRESS/WON/LOST/VOID
    private String requiredFields;
    private Integer isActive;

    @TableLogic
    private Integer deleted;
}
