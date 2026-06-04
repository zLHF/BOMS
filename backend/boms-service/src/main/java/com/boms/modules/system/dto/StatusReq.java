package com.boms.modules.system.dto;

import jakarta.validation.constraints.NotBlank;

public record StatusReq(@NotBlank String status) {}
