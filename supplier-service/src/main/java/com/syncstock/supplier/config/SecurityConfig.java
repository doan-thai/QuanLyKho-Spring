package com.syncstock.supplier.config;

import com.syncstock.common.security.JwtAuthenticationFilter;
import com.syncstock.common.security.SecurityConstants;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/supplier/analytics").hasAnyAuthority(
                                SecurityConstants.ROLE_ADMIN,
                                SecurityConstants.ROLE_THUKHO,
                                SecurityConstants.ROLE_KETOAN
                        )
                        .requestMatchers(HttpMethod.GET, "/api/supplier/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/supplier/**").hasAnyAuthority(
                                SecurityConstants.ROLE_ADMIN,
                                SecurityConstants.ROLE_THUKHO
                        )
                        .requestMatchers(HttpMethod.PUT, "/api/supplier/**").hasAnyAuthority(
                                SecurityConstants.ROLE_ADMIN,
                                SecurityConstants.ROLE_THUKHO
                        )
                        .requestMatchers(HttpMethod.DELETE, "/api/supplier/**").hasAnyAuthority(
                                SecurityConstants.ROLE_ADMIN,
                                SecurityConstants.ROLE_THUKHO
                        )
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("{\"message\":\"Không có quyền truy cập hoặc phiên làm việc đã hết hạn.\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("{\"message\":\"Không có quyền truy cập.\"}");
                        })
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
