package com.boms.modules.opportunity.dto;

/** 阶段配置更新请求。 */
public record StageUpdateReq(
        String name,
        Integer winRate,
        String color,
        Integer isActive
) {}
