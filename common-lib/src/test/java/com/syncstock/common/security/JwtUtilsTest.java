package com.syncstock.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    /** Secret dài đủ 32 byte dùng cho các test case bình thường. */
    private static final String VALID_SECRET = "TestSecret_AtLeast32Chars_ForUnit";

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils(
                VALID_SECRET,
                SecurityConstants.DEFAULT_ISSUER,
                SecurityConstants.DEFAULT_AUDIENCE,
                3600000L
        );
    }

    @Test
    @DisplayName("generateToken + validateToken: token hợp lệ phải pass")
    void testGenerateAndValidateToken() {
        String token = jwtUtils.generateToken(1, "admin", "Admin");
        assertNotNull(token);
        assertTrue(jwtUtils.validateToken(token));

        assertEquals("admin", jwtUtils.getUsername(token));
        assertEquals(1, jwtUtils.getUserId(token));
        assertEquals("Admin", jwtUtils.getRole(token));
    }

    @Test
    @DisplayName("validateToken: token không hợp lệ / null / rỗng -> false")
    void testInvalidToken() {
        assertFalse(jwtUtils.validateToken("invalid.token.here"));
        assertFalse(jwtUtils.validateToken(null));
        assertFalse(jwtUtils.validateToken(""));
    }

    @Test
    @DisplayName("Constructor: secret null -> ném IllegalStateException")
    void constructor_NullSecret_ShouldThrow() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                new JwtUtils(null, SecurityConstants.DEFAULT_ISSUER, SecurityConstants.DEFAULT_AUDIENCE, 3600000L));
        assertTrue(ex.getMessage().contains("JWT secret is not configured"));
    }

    @Test
    @DisplayName("Constructor: secret rỗng -> ném IllegalStateException")
    void constructor_BlankSecret_ShouldThrow() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                new JwtUtils("", SecurityConstants.DEFAULT_ISSUER, SecurityConstants.DEFAULT_AUDIENCE, 3600000L));
        assertTrue(ex.getMessage().contains("JWT secret is not configured"));
    }

    @Test
    @DisplayName("Constructor: secret quá ngắn (< 32 byte) -> ném IllegalStateException")
    void constructor_TooShortSecret_ShouldThrow() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                new JwtUtils("short", SecurityConstants.DEFAULT_ISSUER, SecurityConstants.DEFAULT_AUDIENCE, 3600000L));
        assertTrue(ex.getMessage().contains("too short"));
        assertTrue(ex.getMessage().contains(String.valueOf(SecurityConstants.MIN_SECRET_LENGTH)));
    }

    @Test
    @DisplayName("Constructor: secret đúng 32 byte -> khởi tạo thành công")
    void constructor_ExactlyMinLengthSecret_ShouldSucceed() {
        // 32 ký tự ASCII = 32 byte UTF-8
        String exactly32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456";
        assertDoesNotThrow(() ->
                new JwtUtils(exactly32, SecurityConstants.DEFAULT_ISSUER, SecurityConstants.DEFAULT_AUDIENCE, 3600000L));
    }
}
