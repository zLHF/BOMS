package com.boms.modules.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 签名上传请求。 */
public record SignUploadReq(
        @NotBlank String fileName,
        String contentType,
        @NotBlank String objectType,
        @NotNull Long objectId,
        @NotNull Long fileSize
) {}
