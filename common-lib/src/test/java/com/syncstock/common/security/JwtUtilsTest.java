package com.syncstock.common.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    /** Secret dài đủ 32 byte dùng cho các test case bình thường. Tuyệt đối không dùng DEFAULT_SECRET. */
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
    @DisplayName("token sai issuer -> getClaims ném JwtException, validateToken trả false")
    void token_WrongIssuer_ShouldFail() {
        Date now = new Date();
        String tokenWrongIssuer = Jwts.builder()
                .subject("testuser")
                .issuer("Wrong.Issuer.Service")
                .audience().add(SecurityConstants.DEFAULT_AUDIENCE).and()
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 3600000L))
                .claim(SecurityConstants.CLAIM_USER_ID, 1)
                .claim(SecurityConstants.CLAIM_USERNAME, "testuser")
                .claim(SecurityConstants.CLAIM_ROLE, "Admin")
                .signWith(jwtUtils.getSigningKey())
                .compact();

        assertFalse(jwtUtils.validateToken(tokenWrongIssuer));
        assertThrows(JwtException.class, () -> jwtUtils.getClaims(tokenWrongIssuer));
    }

    @Test
    @DisplayName("token sai audience -> getClaims ném JwtException, validateToken trả false")
    void token_WrongAudience_ShouldFail() {
        Date now = new Date();
        String tokenWrongAudience = Jwts.builder()
                .subject("testuser")
                .issuer(SecurityConstants.DEFAULT_ISSUER)
                .audience().add("Wrong.Audience.Client").and()
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 3600000L))
                .claim(SecurityConstants.CLAIM_USER_ID, 1)
                .claim(SecurityConstants.CLAIM_USERNAME, "testuser")
                .claim(SecurityConstants.CLAIM_ROLE, "Admin")
                .signWith(jwtUtils.getSigningKey())
                .compact();

        assertFalse(jwtUtils.validateToken(tokenWrongAudience));
        assertThrows(JwtException.class, () -> jwtUtils.getClaims(tokenWrongAudience));
    }

    @Test
    @DisplayName("token hết hạn -> getClaims ném ExpiredJwtException, validateToken trả false")
    void token_Expired_ShouldFail() {
        Date past = new Date(System.currentTimeMillis() - 60000L); // 1 phút trước
        String expiredToken = Jwts.builder()
                .subject("testuser")
                .issuer(SecurityConstants.DEFAULT_ISSUER)
                .audience().add(SecurityConstants.DEFAULT_AUDIENCE).and()
                .issuedAt(new Date(past.getTime() - 60000L))
                .expiration(past)
                .claim(SecurityConstants.CLAIM_USER_ID, 1)
                .claim(SecurityConstants.CLAIM_USERNAME, "testuser")
                .claim(SecurityConstants.CLAIM_ROLE, "Admin")
                .signWith(jwtUtils.getSigningKey())
                .compact();

        assertFalse(jwtUtils.validateToken(expiredToken));
        assertThrows(ExpiredJwtException.class, () -> jwtUtils.getClaims(expiredToken));
    }

    @Test
    @DisplayName("token bị sửa payload -> getClaims ném JwtException, validateToken trả false")
    void token_TamperedPayload_ShouldFail() {
        String token = jwtUtils.generateToken(1, "admin", "Admin");
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);

        // Giả mạo payload bằng cách sửa ký tự base64
        String tamperedPayload = parts[1].startsWith("e") ? "a" + parts[1].substring(1) : "e" + parts[1].substring(1);
        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertFalse(jwtUtils.validateToken(tamperedToken));
        assertThrows(JwtException.class, () -> jwtUtils.getClaims(tamperedToken));
    }

    @Test
    @DisplayName("token thiếu claim role -> validateToken pass, getRole trả null")
    void token_MissingRoleClaim_ShouldReturnNullRole() {
        Date now = new Date();
        String tokenWithoutRole = Jwts.builder()
                .subject("testuser")
                .issuer(SecurityConstants.DEFAULT_ISSUER)
                .audience().add(SecurityConstants.DEFAULT_AUDIENCE).and()
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 3600000L))
                .claim(SecurityConstants.CLAIM_USER_ID, 1)
                .claim(SecurityConstants.CLAIM_USERNAME, "testuser")
                .signWith(jwtUtils.getSigningKey())
                .compact();

        assertTrue(jwtUtils.validateToken(tokenWithoutRole));
        assertNull(jwtUtils.getRole(tokenWithoutRole));
        assertEquals("testuser", jwtUtils.getUsername(tokenWithoutRole));
        assertEquals(1, jwtUtils.getUserId(tokenWithoutRole));
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
