package com.syncstock.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
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

        try {
            String token = jwtUtils.extractToken(request);

            if (token != null && jwtUtils.validateToken(token)) {
                String username = jwtUtils.getUsername(token);
                Integer userId = jwtUtils.getUserId(token);
                String role = jwtUtils.getRole(token);

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
            }
        } catch (Exception ex) {
            log.error("Cannot set user authentication: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
