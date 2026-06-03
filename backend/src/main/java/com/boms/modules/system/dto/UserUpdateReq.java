package com.boms.modules.system.dto;

public record UserUpdateReq(
        String realName,
        String mobile,
        String email,
        Long deptId,
        Long directLeaderId
) {}
