package com.boms.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangePwdReq(
        @NotBlank String oldPassword,
        @NotBlank String newPassword
) {}
