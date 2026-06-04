package com.boms.modules.customer.dto;

import jakarta.validation.constraints.NotBlank;

public record ContactReq(
        @NotBlank String name,
        String title,
        String mobile,
        String email,
        Boolean keyPerson
) {}
