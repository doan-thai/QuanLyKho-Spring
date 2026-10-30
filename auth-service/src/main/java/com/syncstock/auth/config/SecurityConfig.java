package com.syncstock.auth.config;

import com.syncstock.common.security.JwtAuthenticationFilter;
import com.syncstock.common.security.SecurityConstants;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Cấu hình Spring Security cho auth-service.
 *
 * <p><b>Kiến trúc định tuyến & CORS:</b></p>
 * <ul>
 *   <li>auth-service là dịch vụ nội bộ (internal microservice), được thiết kế chỉ tiếp nhận request
 *       được ủy quyền và điều hướng từ <code>api-gateway</code> (Port 5000) hoặc nội bộ mạng Docker <code>syncstock-network</code>.</li>
 *   <li>Chính sách CORS (Allowed Origins, Methods, Headers, Credentials) được quản lý tập trung 100% tại
 *       <code>api-gateway</code> (xem <code>com.syncstock.gateway.config.CorsConfig</code>).</li>
 *   <li>CORS tại auth-service được vô hiệu hóa có chủ đích (<code>cors(AbstractHttpConfigurer::disable)</code>) theo quy định
 *       kiến trúc tại <code>RULES.md #2</code> nhằm ngăn chặn triệt để lỗi trùng lặp tiêu đề <code>Access-Control-Allow-Origin</code>
 *       trên trình duyệt phía frontend.</li>
 *   <li>Trong môi trường Docker/Production, cổng của auth-service (8080) không được expose trực tiếp ra ngoài máy chủ host
 *       (chỉ api-gateway expose cổng 5000), đảm bảo mọi truy cập từ bên ngoài bắt buộc phải đi qua gateway.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // CORS bị vô hiệu hóa có chủ đích (by design):
                // auth-service chỉ nhận request được forward từ api-gateway (nơi xử lý CORS tập trung).
                // Bật CORS tại đây sẽ gây trùng lặp header 'Access-Control-Allow-Origin' phía client.
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/logout").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/auth/users/**").hasAuthority(SecurityConstants.ROLE_ADMIN)
                        .requestMatchers("/api/auth/admin").hasAuthority(SecurityConstants.ROLE_ADMIN)
                        .requestMatchers("/api/auth/warehouse").hasAnyAuthority(SecurityConstants.ROLE_ADMIN, SecurityConstants.ROLE_THUKHO)
                        .requestMatchers("/api/auth/report").hasAnyAuthority(SecurityConstants.ROLE_ADMIN, SecurityConstants.ROLE_KETOAN)
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
