package com.boms.modules.opportunity.dto;

import jakarta.validation.constraints.NotNull;

/** 协作人添加请求。 */
public record CollaboratorAddReq(
        @NotNull Long userId,
        String permissionJson
) {}
