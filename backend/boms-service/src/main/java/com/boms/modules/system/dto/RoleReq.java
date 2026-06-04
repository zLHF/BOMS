package com.boms.modules.system.dto;

import jakarta.validation.constraints.NotBlank;

public record RoleReq(
        @NotBlank String name,
        @NotBlank String code,
        String dataScope,
        String remark
) {}
