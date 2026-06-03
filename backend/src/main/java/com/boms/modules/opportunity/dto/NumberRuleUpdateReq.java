package com.boms.modules.opportunity.dto;

/** 编号规则更新请求。 */
public record NumberRuleUpdateReq(
        String prefix,
        String dateFormat,
        Integer seqLength
) {}
