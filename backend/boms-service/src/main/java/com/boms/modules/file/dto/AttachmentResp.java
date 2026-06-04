package com.boms.modules.file.dto;

/** 附件响应 VO。 */
public record AttachmentResp(
        Long id,
        String fileName,
        Long fileSize,
        String contentType,
        String objectType,
        Long objectId,
        String status,
        Long uploaderId,
        String createdAt
) {}
