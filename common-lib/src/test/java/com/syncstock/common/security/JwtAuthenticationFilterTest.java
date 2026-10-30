package com.syncstock.common.security;

import io.jsonwebtoken.Jwts;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthenticationFilterTest {

    private static final String TEST_SECRET = "TestSecret_AtLeast32Chars_ForUnitJwtFilter";

    private JwtUtils jwtUtils;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtUtils = new JwtUtils(
                TEST_SECRET,
                SecurityConstants.DEFAULT_ISSUER,
                SecurityConstants.DEFAULT_AUDIENCE,
                3600000L
        );
        filter = new JwtAuthenticationFilter(jwtUtils);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Token hợp lệ trong Authorization header -> thiết lập authentication và gọi filterChain")
    void validTokenInHeader_ShouldAuthenticateAndProceed() throws ServletException, IOException {
        String token = jwtUtils.generateToken(10, "adminUser", "Admin");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SecurityConstants.HEADER_AUTHORIZATION, SecurityConstants.TOKEN_PREFIX + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("adminUser", SecurityContextHolder.getContext().getAuthentication().getName());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(a -> a.getAuthority().equals("Admin")));
        assertNotNull(filterChain.getRequest());
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("Token hợp lệ trong Cookie -> thiết lập authentication và gọi filterChain")
    void validTokenInCookie_ShouldAuthenticateAndProceed() throws ServletException, IOException {
        String token = jwtUtils.generateToken(20, "cookieUser", "ThuKho");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(SecurityConstants.COOKIE_NAME, token));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("cookieUser", SecurityContextHolder.getContext().getAuthentication().getName());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(a -> a.getAuthority().equals("ThuKho")));
        assertNotNull(filterChain.getRequest());
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("Không có token -> không set authentication và tiếp tục gọi filterChain")
    void noToken_ShouldProceedWithoutAuthentication() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(filterChain.getRequest());
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("Token hết hạn -> trả về 401 Unauthorized và không gọi filterChain tiếp")
    void expiredToken_ShouldReturn401AndHaltChain() throws ServletException, IOException {
        Date past = new Date(System.currentTimeMillis() - 60000L);
        String expiredToken = Jwts.builder()
                .subject("testuser")
                .issuer(SecurityConstants.DEFAULT_ISSUER)
                .audience().add(SecurityConstants.DEFAULT_AUDIENCE).and()
                .issuedAt(new Date(past.getTime() - 60000L))
                .expiration(past)
                .claim(SecurityConstants.CLAIM_ROLE, "Admin")
                .signWith(jwtUtils.getSigningKey())
                .compact();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SecurityConstants.HEADER_AUTHORIZATION, SecurityConstants.TOKEN_PREFIX + expiredToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(filterChain.getRequest(), "filterChain không được gọi tiếp khi token hết hạn");
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Không có quyền truy cập hoặc phiên làm việc đã hết hạn."));
    }

    @Test
    @DisplayName("Token sai issuer -> trả về 401 Unauthorized và không gọi filterChain tiếp")
    void wrongIssuerToken_ShouldReturn401AndHaltChain() throws ServletException, IOException {
        Date now = new Date();
        String wrongIssuerToken = Jwts.builder()
                .subject("testuser")
                .issuer("Wrong.Issuer")
                .audience().add(SecurityConstants.DEFAULT_AUDIENCE).and()
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 3600000L))
                .claim(SecurityConstants.CLAIM_ROLE, "Admin")
                .signWith(jwtUtils.getSigningKey())
                .compact();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SecurityConstants.HEADER_AUTHORIZATION, SecurityConstants.TOKEN_PREFIX + wrongIssuerToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(filterChain.getRequest());
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Không có quyền truy cập hoặc phiên làm việc đã hết hạn."));
    }

    @Test
    @DisplayName("Token sai audience -> trả về 401 Unauthorized và không gọi filterChain tiếp")
    void wrongAudienceToken_ShouldReturn401AndHaltChain() throws ServletException, IOException {
        Date now = new Date();
        String wrongAudienceToken = Jwts.builder()
                .subject("testuser")
                .issuer(SecurityConstants.DEFAULT_ISSUER)
                .audience().add("Wrong.Audience").and()
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 3600000L))
                .claim(SecurityConstants.CLAIM_ROLE, "Admin")
                .signWith(jwtUtils.getSigningKey())
                .compact();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SecurityConstants.HEADER_AUTHORIZATION, SecurityConstants.TOKEN_PREFIX + wrongAudienceToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(filterChain.getRequest());
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Không có quyền truy cập hoặc phiên làm việc đã hết hạn."));
    }

    @Test
    @DisplayName("Token bị sửa payload -> trả về 401 Unauthorized và không gọi filterChain tiếp")
    void tamperedPayloadToken_ShouldReturn401AndHaltChain() throws ServletException, IOException {
        String token = jwtUtils.generateToken(1, "admin", "Admin");
        String[] parts = token.split("\\.");
        String tamperedPayload = parts[1].startsWith("e") ? "a" + parts[1].substring(1) : "e" + parts[1].substring(1);
        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SecurityConstants.HEADER_AUTHORIZATION, SecurityConstants.TOKEN_PREFIX + tamperedToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(filterChain.getRequest());
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Không có quyền truy cập hoặc phiên làm việc đã hết hạn."));
    }

    @Test
    @DisplayName("Token thiếu claim role -> authenticate thành công với authorities rỗng và gọi filterChain")
    void missingRoleClaim_ShouldAuthenticateWithEmptyAuthorities() throws ServletException, IOException {
        Date now = new Date();
        String tokenWithoutRole = Jwts.builder()
                .subject("userNoRole")
                .issuer(SecurityConstants.DEFAULT_ISSUER)
                .audience().add(SecurityConstants.DEFAULT_AUDIENCE).and()
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 3600000L))
                .claim(SecurityConstants.CLAIM_USER_ID, 99)
                .claim(SecurityConstants.CLAIM_USERNAME, "userNoRole")
                .signWith(jwtUtils.getSigningKey())
                .compact();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SecurityConstants.HEADER_AUTHORIZATION, SecurityConstants.TOKEN_PREFIX + tokenWithoutRole);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("userNoRole", SecurityContextHolder.getContext().getAuthentication().getName());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().isEmpty());
        assertNotNull(filterChain.getRequest());
        assertEquals(200, response.getStatus());
    }
}

