package com.boms.modules.system.dto;

import java.util.List;

public record AssignPermReq(
        List<Long> permissionIds,
        String dataScope
) {}
