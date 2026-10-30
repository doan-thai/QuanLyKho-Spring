package com.syncstock.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncstock.auth.config.SecurityConfig;
import com.syncstock.auth.dto.RegisterRequest;
import com.syncstock.auth.dto.RegisterResponse;
import com.syncstock.auth.dto.UserResponse;
import com.syncstock.auth.service.AuthService;
import com.syncstock.common.exception.GlobalExceptionHandler;
import com.syncstock.common.security.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            ServletRequest req = invocation.getArgument(0);
            ServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    @DisplayName("M-05: POST /api/auth/register trả về 201 Created khi đăng ký hợp lệ")
    void register_ValidRequest_ShouldReturn201Created() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("validUser")
                .password("Password123")
                .role("ThuKho")
                .build();

        UserResponse userResponse = new UserResponse(1, "validUser", "ThuKho", true);
        RegisterResponse registerResponse = new RegisterResponse("Đăng ký thành công", userResponse);

        when(authService.register(any(RegisterRequest.class))).thenReturn(registerResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Đăng ký thành công"))
                .andExpect(jsonPath("$.user.username").value("validUser"))
                .andExpect(jsonPath("$.user.role").value("ThuKho"));
    }

    @Test
    @DisplayName("M-06: POST /api/auth/register trả về 400 Bad Request khi mật khẩu yếu")
    void register_WeakPassword_ShouldReturn400BadRequest() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("validUser")
                .password("123456") // yếu: không có chữ hoa, không có chữ thường, độ dài < 8
                .role("ThuKho")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
