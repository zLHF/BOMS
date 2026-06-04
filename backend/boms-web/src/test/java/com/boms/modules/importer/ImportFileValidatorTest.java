package com.boms.modules.importer;

import com.boms.common.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** ImportFileValidator 批量导入入口校验测试。 */
class ImportFileValidatorTest {

    @Test
    void acceptsExcelExtensionsCaseInsensitive() {
        assertDoesNotThrow(() -> ImportFileValidator.validate("商机.XLSX", 1024, 10 * 1024));
        assertDoesNotThrow(() -> ImportFileValidator.validate("商机.xls", 1024, 10 * 1024));
    }

    @Test
    void rejectsNonExcelFile() {
        BizException e = assertThrows(BizException.class,
                () -> ImportFileValidator.validate("payload.txt", 1024, 10 * 1024));
        assertEquals(40002, e.getCode());
    }

    @Test
    void rejectsEmptyOrOversizedFile() {
        assertEquals(40001, assertThrows(BizException.class,
                () -> ImportFileValidator.validate("empty.xlsx", 0, 10 * 1024)).getCode());
        assertEquals(40003, assertThrows(BizException.class,
                () -> ImportFileValidator.validate("large.xlsx", 20 * 1024, 10 * 1024)).getCode());
    }
}
