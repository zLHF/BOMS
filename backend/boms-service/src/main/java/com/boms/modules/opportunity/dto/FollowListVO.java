package com.boms.modules.opportunity.dto;

import com.boms.modules.opportunity.entity.OpportunityFollow;

/** 跟进记录列表项（富化商机标题和创建人姓名）。 */
public record FollowListVO(
        Long id,
        Long opportunityId,
        String opportunityTitle,
        String followType,
        String content,
        String result,
        String nextTime,
        Long creatorId,
        String creatorName,
        String createdAt
) {
    public static FollowListVO from(OpportunityFollow f, String opportunityTitle, String creatorName) {
        return new FollowListVO(
                f.getId(),
                f.getOpportunityId(),
                opportunityTitle,
                f.getFollowType(),
                f.getContent(),
                f.getResult(),
                f.getNextTime() != null ? f.getNextTime().toString() : null,
                f.getCreatorId(),
                creatorName,
                f.getCreatedAt() != null ? f.getCreatedAt().toString() : null
        );
    }
}
