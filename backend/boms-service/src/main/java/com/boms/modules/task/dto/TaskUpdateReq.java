package com.boms.modules.task.dto;

/** 任务更新请求。 */
public record TaskUpdateReq(
        String title,
        String content,
        String priority,
        String dueAt
) {}
