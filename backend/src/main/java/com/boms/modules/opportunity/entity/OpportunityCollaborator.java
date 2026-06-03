package com.boms.modules.opportunity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 商机协作（M09），映射 opportunity_collaborator 表。 */
@Data
@TableName("opportunity_collaborator")
public class OpportunityCollaborator {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long opportunityId;
    private Long userId;
    private String permissionJson;
    private String status;       // ACTIVE / REMOVED
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
