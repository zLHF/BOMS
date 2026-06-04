package com.boms.modules.opportunity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record OpportunityReq(
        @NotBlank String title,
        @NotNull Long customerId,
        Long stageId,
        String source,
        BigDecimal amount,
        Integer winRate,
        Long ownerId,
        String demand,
        String expectedCloseAt
) {}
