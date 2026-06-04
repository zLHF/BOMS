package com.boms.modules.opportunity.dto;

import jakarta.validation.constraints.NotNull;

public record TransferReq(@NotNull Long ownerId) {}
