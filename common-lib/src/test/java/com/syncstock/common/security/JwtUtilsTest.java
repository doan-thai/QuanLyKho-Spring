package com.syncstock.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils(
                SecurityConstants.DEFAULT_SECRET,
                SecurityConstants.DEFAULT_ISSUER,
                SecurityConstants.DEFAULT_AUDIENCE,
                3600000L
        );
    }

    @Test
    void testGenerateAndValidateToken() {
        String token = jwtUtils.generateToken(1, "admin", "Admin");
        assertNotNull(token);
        assertTrue(jwtUtils.validateToken(token));

        assertEquals("admin", jwtUtils.getUsername(token));
        assertEquals(1, jwtUtils.getUserId(token));
        assertEquals("Admin", jwtUtils.getRole(token));
    }

    @Test
    void testInvalidToken() {
        assertFalse(jwtUtils.validateToken("invalid.token.here"));
        assertFalse(jwtUtils.validateToken(null));
        assertFalse(jwtUtils.validateToken(""));
    }
}
