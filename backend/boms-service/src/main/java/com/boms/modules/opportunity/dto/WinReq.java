package com.boms.modules.opportunity.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record WinReq(
        @NotNull BigDecimal dealAmount,
        @NotNull String dealAt,
        @NotNull String note
) {}
