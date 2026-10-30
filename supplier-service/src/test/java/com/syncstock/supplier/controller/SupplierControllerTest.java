package com.syncstock.supplier.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncstock.common.dto.PagedResult;
import com.syncstock.common.exception.GlobalExceptionHandler;
import com.syncstock.common.security.JwtAuthenticationFilter;
import com.syncstock.supplier.config.SecurityConfig;
import com.syncstock.supplier.dto.SupplierAnalyticsDto;
import com.syncstock.supplier.dto.SupplierUpsertRequest;
import com.syncstock.supplier.entity.SupplierInfo;
import com.syncstock.supplier.service.SupplierService;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SupplierController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class SupplierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SupplierService supplierService;

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
    @DisplayName("Chưa đăng nhập -> GET /api/supplier trả 401 Unauthorized")
    void getAll_Unauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/supplier"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Đã đăng nhập -> GET /api/supplier trả 200 OK với PagedResult chứa totalRecords")
    @WithMockUser(authorities = {"ThuKho"})
    void getAll_Authenticated_ShouldReturnPagedResult() throws Exception {
        SupplierInfo mockInfo = SupplierInfo.builder()
                .id(1)
                .name("Công ty ABC")
                .phone("0900000001")
                .build();

        PagedResult<SupplierInfo> paged = PagedResult.<SupplierInfo>builder()
                .totalRecords(1)
                .pageNumber(1)
                .pageSize(50)
                .totalPages(1)
                .data(List.of(mockInfo))
                .build();

        when(supplierService.getAll(any(), anyInt(), anyInt())).thenReturn(paged);

        mockMvc.perform(get("/api/supplier?pageNumber=1&pageSize=50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords").value(1))
                .andExpect(jsonPath("$.pageNumber").value(1))
                .andExpect(jsonPath("$.pageSize").value(50))
                .andExpect(jsonPath("$.data[0].name").value("Công ty ABC"));
    }

    @Test
    @DisplayName("Role KeToan -> POST /api/supplier bị chặn 403 Forbidden")
    @WithMockUser(authorities = {"KeToan"})
    void create_WithKeToan_ShouldReturn403() throws Exception {
        SupplierUpsertRequest request = SupplierUpsertRequest.builder()
                .name("Công ty Mới")
                .phone("0912345678")
                .build();

        mockMvc.perform(post("/api/supplier")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Role ThuKho -> POST /api/supplier với phone sai Regex -> Trả 400 Bad Request")
    @WithMockUser(authorities = {"ThuKho"})
    void create_InvalidPhoneRegex_ShouldReturn400() throws Exception {
        SupplierUpsertRequest request = SupplierUpsertRequest.builder()
                .name("Công ty Mới")
                .phone("12345") // Không khớp regex ^(0\d{9}|\+84\d{9})$
                .build();

        mockMvc.perform(post("/api/supplier")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Role ThuKho -> POST /api/supplier hợp lệ -> Trả 201 Created")
    @WithMockUser(authorities = {"ThuKho"})
    void create_ValidRequest_ShouldReturn201() throws Exception {
        SupplierUpsertRequest request = SupplierUpsertRequest.builder()
                .name("Công ty Hợp Lệ")
                .phone("0912345678")
                .address("Hà Nội")
                .build();

        SupplierInfo mockCreated = SupplierInfo.builder()
                .id(1)
                .name("Công ty Hợp Lệ")
                .phone("0912345678")
                .address("Hà Nội")
                .createdAt(LocalDateTime.now())
                .totalTransactions(0)
                .totalImportValue(BigDecimal.ZERO)
                .reliabilityScore(new BigDecimal("50.00"))
                .build();

        when(supplierService.create(any(SupplierUpsertRequest.class))).thenReturn(mockCreated);

        mockMvc.perform(post("/api/supplier")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Công ty Hợp Lệ"));
    }

    @Test
    @DisplayName("Role KeToan -> GET /api/supplier/analytics -> Trả 200 OK")
    @WithMockUser(authorities = {"KeToan"})
    void getAnalytics_WithKeToan_ShouldReturn200() throws Exception {
        SupplierAnalyticsDto mockInfo = SupplierAnalyticsDto.builder()
                .id(1)
                .name("Công ty ABC")
                .phone("0900000001")
                .totalTransactions(5)
                .totalImportValue(new BigDecimal("50000000"))
                .reliabilityScore(new BigDecimal("60.00"))
                .build();

        when(supplierService.getAnalytics(eq(2000), any())).thenReturn(List.of(mockInfo));

        mockMvc.perform(get("/api/supplier/analytics?limit=2000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Công ty ABC"))
                .andExpect(jsonPath("$[0].totalTransactions").value(5));
    }
}
