package com.syncstock.report.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.syncstock.common.dto.PagedResult;
import com.syncstock.report.config.RestClientConfig;
import com.syncstock.report.dto.StockReportItemDto;
import com.syncstock.report.dto.TransactionReportItemDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class InventoryReportClientTest {

    private ObjectMapper objectMapper;
    private RestClientConfig restClientConfig;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        restClientConfig = new RestClientConfig();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("M-14: InventoryReportClient chuyển tiếp Bearer token từ caller qua RequestContextHolder")
    void getStock_ShouldForwardBearerTokenFromRequestContext() throws Exception {
        // Giả lập HTTP request hiện tại của caller mang token
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer caller-jwt-token-xyz");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RestClient.Builder builder = restClientConfig.restClientBuilder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        InventoryReportClient client = new InventoryReportClient(builder, "http://localhost:5002");

        PagedResult<StockReportItemDto> mockResponse = PagedResult.<StockReportItemDto>builder()
                .totalRecords(1)
                .pageNumber(1)
                .pageSize(100)
                .totalPages(1)
                .data(List.of(
                        StockReportItemDto.builder()
                                .id(1)
                                .name("Bàn phím cơ")
                                .category("Điện tử")
                                .stock(15)
                                .minStock(10)
                                .isLowStock(false)
                                .build()
                ))
                .build();

        server.expect(requestTo("http://localhost:5002/api/inventory/stock"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer caller-jwt-token-xyz"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        List<StockReportItemDto> stock = client.getStock();

        server.verify();
        assertNotNull(stock);
        assertEquals(1, stock.size());
        assertEquals("Bàn phím cơ", stock.get(0).getName());
    }

    @Test
    @DisplayName("M-14: InventoryReportClient chuyển tiếp Bearer token được truyền trực tiếp")
    void getHistory_ShouldForwardExplicitBearerToken() throws Exception {
        RestClient.Builder builder = restClientConfig.restClientBuilder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        InventoryReportClient client = new InventoryReportClient(builder, "http://localhost:5002");

        PagedResult<TransactionReportItemDto> mockResponse = PagedResult.<TransactionReportItemDto>builder()
                .totalRecords(1)
                .pageNumber(1)
                .pageSize(100)
                .totalPages(1)
                .data(List.of(
                        TransactionReportItemDto.builder()
                                .id(101)
                                .productId(1)
                                .productName("Bàn phím cơ")
                                .type("IMPORT")
                                .quantity(10)
                                .unitPrice(BigDecimal.valueOf(500_000))
                                .totalAmount(BigDecimal.valueOf(5_000_000))
                                .date(LocalDateTime.now())
                                .build()
                ))
                .build();

        server.expect(requestTo("http://localhost:5002/api/inventory/history?limit=100"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer explicit-token-abc"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        List<TransactionReportItemDto> history = client.getHistory(100, "explicit-token-abc");

        server.verify();
        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals("Bàn phím cơ", history.get(0).getProductName());
    }
}

