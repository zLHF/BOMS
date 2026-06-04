package com.boms.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** BizException 测试。 */
class BizExceptionTest {

    @Test
    void createWithCodeAndMessage() {
        BizException e = new BizException(40400, "商机不存在");
        assertEquals(40400, e.getCode());
        assertEquals("商机不存在", e.getMessage());
    }

    @Test
    void createPlatformAdmin() {
        BizException e = new BizException(40310, "无权操作");
        assertEquals(40310, e.getCode());
        assertEquals("无权操作", e.getMessage());
        assertNull(e.getCause());
    }
}
