package com.syncstock.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = jwtUtils.extractToken(request);

        if (token != null && !token.isBlank()) {
            try {
                Claims claims = jwtUtils.getClaims(token);

                String username = claims.get(SecurityConstants.CLAIM_USERNAME, String.class);
                if (username == null || username.isBlank()) {
                    username = claims.getSubject();
                }

                Object userIdObj = claims.get(SecurityConstants.CLAIM_USER_ID);
                Integer userId = null;
                if (userIdObj instanceof Number number) {
                    userId = number.intValue();
                } else if (userIdObj instanceof String str) {
                    try {
                        userId = Integer.parseInt(str);
                    } catch (NumberFormatException ignored) {
                        userId = null;
                    }
                }

                String role = claims.get(SecurityConstants.CLAIM_ROLE, String.class);

                List<SimpleGrantedAuthority> authorities = (role != null && !role.isBlank())
                        ? List.of(new SimpleGrantedAuthority(role))
                        : Collections.emptyList();

                UserPrincipal principal = UserPrincipal.builder()
                        .userId(userId)
                        .username(username)
                        .role(role)
                        .active(true)
                        .build();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authenticated user: '{}' with role: '{}'", username, role);
            } catch (ExpiredJwtException ex) {
                log.warn("JWT token expired: {}", ex.getMessage());
                sendUnauthorizedResponse(response);
                return;
            } catch (JwtException ex) {
                log.warn("Invalid JWT token: {}", ex.getMessage());
                sendUnauthorizedResponse(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorizedResponse(HttpServletResponse response) throws IOException {
        if (!response.isCommitted()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"message\":\"Không có quyền truy cập hoặc phiên làm việc đã hết hạn.\"}");
        }
    }
}
