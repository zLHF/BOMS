package com.boms.modules.file.dto;

import java.util.Map;

/** 签名上传响应。 */
public record SignUploadResp(
        Long attachmentId,
        String uploadUrl,
        Map<String, String> headers
) {}
