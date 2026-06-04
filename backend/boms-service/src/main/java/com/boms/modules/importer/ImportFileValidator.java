package com.boms.modules.importer;

import com.boms.common.exception.BizException;

import java.util.Locale;

/** 批量导入文件基础校验，避免非 Excel/超大文件进入解析链路。 */
public final class ImportFileValidator {

    private ImportFileValidator() {
    }

    public static void validate(String originalFilename, long fileSize, long maxFileSizeBytes) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BizException(40001, "请选择要导入的文件");
        }
        String lowerName = originalFilename.toLowerCase(Locale.ROOT);
        if (!lowerName.endsWith(".xlsx") && !lowerName.endsWith(".xls")) {
            throw new BizException(40002, "仅支持 Excel 文件（.xlsx / .xls）");
        }
        if (fileSize <= 0) {
            throw new BizException(40001, "请选择要导入的文件");
        }
        if (maxFileSizeBytes > 0 && fileSize > maxFileSizeBytes) {
            throw new BizException(40003, "导入文件大小超出限制");
        }
    }
}
