package com.syncstock.auth.config;

import com.syncstock.common.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("PasswordEncoder bean - mã hóa và kiểm tra mật khẩu chính xác bằng BCrypt")
    void passwordEncoder_ShouldEncodeAndMatchPassword() {
        SecurityConfig config = new SecurityConfig(jwtAuthenticationFilter);
        PasswordEncoder encoder = config.passwordEncoder();

        assertThat(encoder).isNotNull();
        String rawPassword = "TestPassword@123";
        String encoded = encoder.encode(rawPassword);

        assertThat(encoded)
                .isNotBlank()
                .isNotEqualTo(rawPassword)
                .startsWith("$2a$");

        assertThat(encoder.matches(rawPassword, encoded)).isTrue();
        assertThat(encoder.matches("WrongPassword", encoded)).isFalse();
    }
}

