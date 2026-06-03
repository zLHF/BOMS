package com.boms.modules.opportunity.dto;

import jakarta.validation.constraints.NotBlank;

public record LoseReq(
        @NotBlank String reason,
        String competitor,
        String review
) {}
