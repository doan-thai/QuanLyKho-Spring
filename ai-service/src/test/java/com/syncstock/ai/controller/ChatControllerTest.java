package com.syncstock.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncstock.ai.dto.ChatRequest;
import com.syncstock.ai.dto.ChatResponse;
import com.syncstock.ai.service.AiService;
import com.syncstock.common.security.JwtUtils;
import com.syncstock.common.security.SecurityConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "jwt.secret=TestSecretKeyForAiServiceMustBeAtLeast32BytesLong"
})
@AutoConfigureMockMvc
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @MockBean
    private AiService aiService;

    private String validToken;

    @BeforeEach
    void setUp() {
        validToken = "Bearer " + jwtUtils.generateToken(1, "testuser", SecurityConstants.ROLE_THUKHO);
    }

    @Test
    @DisplayName("POST /api/chat - Đã xác thực trả về 200 và câu trả lời AI")
    void postChat_Authenticated_Returns200AndAnswer() throws Exception {
        ChatRequest request = ChatRequest.builder().message("Tồn kho bàn phím còn bao nhiêu?").build();
        ChatResponse mockResponse = ChatResponse.builder().answer("Bàn phím cơ hiện còn 15 chiếc trong kho.").build();

        when(aiService.chat(any(ChatRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/chat")
                        .header(HttpHeaders.AUTHORIZATION, validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Bàn phím cơ hiện còn 15 chiếc trong kho."));
    }

    @Test
    @DisplayName("POST /api/chat/ask - Đã xác thực trả về 200 và câu trả lời AI")
    void postAsk_Authenticated_Returns200AndAnswer() throws Exception {
        ChatRequest request = ChatRequest.builder().message("Mặt hàng nào đang thiếu hàng?").build();
        ChatResponse mockResponse = ChatResponse.builder().answer("Hiện có 2 mặt hàng sắp hết là Chuột và Bàn phím.").build();

        when(aiService.chat(any(ChatRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/chat/ask")
                        .header(HttpHeaders.AUTHORIZATION, validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Hiện có 2 mặt hàng sắp hết là Chuột và Bàn phím."));
    }

    @Test
    @DisplayName("POST /api/chat/summary - Đã xác thực trả về 200 và tóm tắt kho")
    void postSummary_Authenticated_Returns200AndSummary() throws Exception {
        ChatResponse mockResponse = ChatResponse.builder().answer("1. Tổng mặt hàng: 20.\n2. Tồn kho: 500.").build();

        when(aiService.generateSummary()).thenReturn(mockResponse);

        mockMvc.perform(post("/api/chat/summary")
                        .header(HttpHeaders.AUTHORIZATION, validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("1. Tổng mặt hàng: 20.\n2. Tồn kho: 500."));
    }

    @Test
    @DisplayName("POST /api/chat - Chưa xác thực trả về 401 Unauthorized")
    void postChat_Unauthenticated_Returns401() throws Exception {
        ChatRequest request = ChatRequest.builder().message("Test").build();

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
