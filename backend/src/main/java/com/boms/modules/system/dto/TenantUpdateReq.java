package com.boms.modules.system.dto;

public record TenantUpdateReq(
        String name,
        String domain,
        String expireAt
) {}
