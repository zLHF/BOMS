package com.boms.common.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** R 统一响应测试。 */
class RTest {

    @Test
    void okWithData() {
        R<String> r = R.ok("hello");
        assertEquals(0, r.getCode());
        assertEquals("ok", r.getMessage());
        assertEquals("hello", r.getData());
    }

    @Test
    void okNull() {
        R<Void> r = R.ok();
        assertEquals(0, r.getCode());
        assertNull(r.getData());
    }

    @Test
    void failWithCodeAndMessage() {
        R<Void> r = R.fail(40101, "账号或密码错误");
        assertEquals(40101, r.getCode());
        assertEquals("账号或密码错误", r.getMessage());
        assertNull(r.getData());
    }
}
