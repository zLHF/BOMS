package com.boms.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record UserCreateReq(
        @NotBlank String username,
        String mobile,
        String email,
        @NotBlank String realName,
        Long deptId,
        Long directLeaderId,
        String password,
        List<Long> roleIds
) {}
