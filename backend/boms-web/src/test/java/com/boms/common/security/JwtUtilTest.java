package com.boms.common.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** JwtUtil 签发/解析测试。 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(
                "boms-phase0-demo-secret-key-change-me-in-prod-0123456789",
                720L
        );
    }

    @Test
    void issueAndParse() {
        String token = jwtUtil.issue(1L, 100L, "zhangwei");

        Claims claims = jwtUtil.parse(token);
        assertEquals("1", claims.getSubject());
        assertEquals(100L, claims.get("tid", Number.class).longValue());
        assertEquals("zhangwei", claims.get("uname", String.class));
    }

    @Test
    void parseInvalidTokenThrows() {
        assertThrows(Exception.class, () -> jwtUtil.parse("invalid.token.here"));
    }

    @Test
    void differentUsersDifferentTokens() {
        String t1 = jwtUtil.issue(1L, 100L, "user1");
        String t2 = jwtUtil.issue(2L, 100L, "user2");

        Claims c1 = jwtUtil.parse(t1);
        Claims c2 = jwtUtil.parse(t2);

        assertNotEquals(c1.getSubject(), c2.getSubject());
    }
}
