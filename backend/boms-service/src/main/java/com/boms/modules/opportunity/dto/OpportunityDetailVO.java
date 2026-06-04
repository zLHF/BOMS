package com.boms.modules.opportunity.dto;

import com.boms.modules.opportunity.entity.Opportunity;

/** 商机详情富化 VO（关联客户名/阶段名/负责人名）。 */
public record OpportunityDetailVO(
        Opportunity opportunity,
        String customerName,
        String stageName,
        String ownerName
) {}
