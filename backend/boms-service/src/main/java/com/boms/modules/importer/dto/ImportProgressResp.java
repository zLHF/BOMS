package com.boms.modules.importer.dto;

/** 导入进度响应。 */
public record ImportProgressResp(
        Long taskId,
        String status,
        Integer total,
        Integer success,
        Integer failed
) {}
