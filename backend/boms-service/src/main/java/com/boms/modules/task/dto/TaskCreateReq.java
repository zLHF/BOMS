package com.boms.modules.task.dto;

import jakarta.validation.constraints.NotBlank;

/** 任务创建请求。 */
public record TaskCreateReq(
        @NotBlank String title,
        String content,
        String objectType,
        Long objectId,
        Long assigneeId,
        String dueAt,
        String priority
) {}
