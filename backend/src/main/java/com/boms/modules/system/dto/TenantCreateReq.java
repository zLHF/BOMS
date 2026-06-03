package com.boms.modules.system.dto;

import jakarta.validation.constraints.NotBlank;

public record TenantCreateReq(
        @NotBlank String name,
        @NotBlank String code,
        String domain,
        Long packageId,
        String expireAt,
        @NotBlank String adminUsername,
        String adminMobile,
        String adminPassword
) {}
