package com.syncstock.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {

    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);

    private final String secret;
    private final String issuer;
    private final String audience;
    private final long expirationMs;

    public JwtUtils(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer:" + SecurityConstants.DEFAULT_ISSUER + "}") String issuer,
            @Value("${jwt.audience:" + SecurityConstants.DEFAULT_AUDIENCE + "}") String audience,
            @Value("${jwt.expiration-ms:3600000}") long expirationMs) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "[SECURITY] JWT secret is not configured. Set the JWT_SECRET environment variable.");
        }
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < SecurityConstants.MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "[SECURITY] JWT secret is too short (" + secretBytes.length + " bytes). " +
                            "Minimum required: " + SecurityConstants.MIN_SECRET_LENGTH + " bytes. " +
                            "Please set a stronger JWT_SECRET.");
        }
        this.secret = secret;
        this.issuer = issuer;
        this.audience = audience;
        this.expirationMs = expirationMs;
    }

    public SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Integer userId, String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .issuer(issuer)
                .audience().add(audience).and()
                .issuedAt(now)
                .expiration(expiryDate)
                .claim(SecurityConstants.CLAIM_USER_ID, userId)
                .claim(SecurityConstants.CLAIM_USERNAME, username)
                .claim(SecurityConstants.CLAIM_UNIQUE_NAME, username)
                .claim(SecurityConstants.CLAIM_ROLE, role)
                .signWith(getSigningKey())
                .compact();
    }

    public Claims getClaims(String token) {
        var parserBuilder = Jwts.parser()
                .verifyWith(getSigningKey());
        if (issuer != null && !issuer.isBlank()) {
            parserBuilder.requireIssuer(issuer);
        }
        if (audience != null && !audience.isBlank()) {
            parserBuilder.requireAudience(audience);
        }
        return parserBuilder.build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Invalid JWT token: {}", ex.getMessage());
            return false;
        }
    }

    public String getUsername(String token) {
        Claims claims = getClaims(token);
        String username = claims.get(SecurityConstants.CLAIM_USERNAME, String.class);
        if (username != null && !username.isBlank()) {
            return username;
        }
        return claims.getSubject();
    }

    public Integer getUserId(String token) {
        Claims claims = getClaims(token);
        Object userIdObj = claims.get(SecurityConstants.CLAIM_USER_ID);
        if (userIdObj instanceof Number number) {
            return number.intValue();
        } else if (userIdObj instanceof String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public String getRole(String token) {
        Claims claims = getClaims(token);
        return claims.get(SecurityConstants.CLAIM_ROLE, String.class);
    }

    public String extractToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        // 1. Kiểm tra Header Authorization: Bearer <token>
        String authHeader = request.getHeader(SecurityConstants.HEADER_AUTHORIZATION);
        if (authHeader != null && authHeader.regionMatches(true, 0, SecurityConstants.TOKEN_PREFIX, 0,
                SecurityConstants.TOKEN_PREFIX.length())) {
            String token = authHeader.substring(SecurityConstants.TOKEN_PREFIX.length()).trim();
            if (!token.isEmpty()) {
                return token;
            }
        }

        // 2. Kiểm tra Cookie access_token
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (SecurityConstants.COOKIE_NAME.equals(cookie.getName()) && cookie.getValue() != null) {
                    String token = cookie.getValue().trim();
                    if (!token.isEmpty()) {
                        return token;
                    }
                }
            }
        }

        return null;
    }

    String getSecret() {
        return secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getAudience() {
        return audience;
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
