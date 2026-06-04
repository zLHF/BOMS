package com.boms.modules.task.dto;

import jakarta.validation.constraints.NotNull;

/** 任务指派请求。 */
public record TaskAssignReq(
        @NotNull Long assigneeId
) {}
