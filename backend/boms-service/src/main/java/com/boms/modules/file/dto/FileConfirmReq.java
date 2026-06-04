package com.boms.modules.file.dto;

import jakarta.validation.constraints.NotNull;

/** 确认上传完成请求。 */
public record FileConfirmReq(
        @NotNull Long attachmentId
) {}
