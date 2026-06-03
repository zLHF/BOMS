package com.boms.modules.customer.dto;

import jakarta.validation.constraints.NotBlank;

public record CustomerReq(
        @NotBlank String name,
        String creditCode,
        String type,
        String industry,
        String region,
        String level,
        Long ownerId,
        Long deptId,
        Long parentId,
        Boolean force
) {}
