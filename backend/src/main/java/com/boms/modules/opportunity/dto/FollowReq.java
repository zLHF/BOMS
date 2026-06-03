package com.boms.modules.opportunity.dto;

import jakarta.validation.constraints.NotBlank;

public record FollowReq(
        String followType,
        @NotBlank String content,
        String result,
        String nextTime
) {}
