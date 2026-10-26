package com.syncstock.report.controller;

import com.syncstock.common.security.JwtUtils;
import com.syncstock.common.security.SecurityConstants;
import com.syncstock.report.dto.BusinessReportResponse;
import com.syncstock.report.dto.StockReportItemDto;
import com.syncstock.report.dto.StockReportResponse;
import com.syncstock.report.dto.TopSellingProductDto;
import com.syncstock.report.dto.TransactionReportItemDto;
import com.syncstock.report.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @MockBean
    private ReportService reportService;

    private String adminToken;
    private String keToanToken;
    private String thuKhoToken;

    @BeforeEach
    void setUp() {
        adminToken = "Bearer " + jwtUtils.generateToken(1, "admin", SecurityConstants.ROLE_ADMIN);
        keToanToken = "Bearer " + jwtUtils.generateToken(2, "ketoan", SecurityConstants.ROLE_KETOAN);
        thuKhoToken = "Bearer " + jwtUtils.generateToken(3, "thukho", SecurityConstants.ROLE_THUKHO);
    }

    @Test
    @DisplayName("GET /api/report/stock - Quyền Admin thành công trả về 200")
    void getStockReport_Admin_Returns200() throws Exception {
        StockReportResponse mockResponse = StockReportResponse.builder()
                .totalProducts(2)
                .totalStock(150L)
                .lowStockCount(1)
                .items(List.of(
                        StockReportItemDto.builder().id(1).name("Chuột không dây").category("Điện tử").stock(100).minStock(10).isLowStock(false).build(),
                        StockReportItemDto.builder().id(2).name("Bàn phím cơ").category("Điện tử").stock(5).minStock(20).isLowStock(true).build()
                ))
                .build();

        when(reportService.getStockReport()).thenReturn(mockResponse);

        mockMvc.perform(get("/api/report/stock")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(2))
                .andExpect(jsonPath("$.totalStock").value(150))
                .andExpect(jsonPath("$.lowStockCount").value(1))
                .andExpect(jsonPath("$.alertsCount").value(1))
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/report/transactions - Quyền KeToan thành công trả về 200")
    void getTransactions_KeToan_Returns200() throws Exception {
        List<TransactionReportItemDto> mockList = List.of(
                TransactionReportItemDto.builder()
                        .id(101)
                        .productId(1)
                        .productName("Chuột không dây")
                        .type("IMPORT")
                        .quantity(50)
                        .unitPrice(new BigDecimal("150000.00"))
                        .totalAmount(new BigDecimal("7500000.00"))
                        .date(LocalDateTime.now())
                        .build()
        );

        when(reportService.getTransactions(
                org.mockito.ArgumentMatchers.nullable(String.class),
                org.mockito.ArgumentMatchers.nullable(String.class),
                org.mockito.ArgumentMatchers.nullable(String.class),
                anyInt()
        )).thenReturn(mockList);

        mockMvc.perform(get("/api/report/transactions?from=2026-04-01&to=2026-04-30&type=IMPORT&limit=100")
                        .header(HttpHeaders.AUTHORIZATION, keToanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productName").value("Chuột không dây"))
                .andExpect(jsonPath("$[0].type").value("IMPORT"));
    }

    @Test
    @DisplayName("GET /api/report/business - Quyền Admin thành công tính doanh thu, chi phí, lợi nhuận gộp")
    void getBusinessReport_Admin_Returns200() throws Exception {
        BusinessReportResponse mockResponse = BusinessReportResponse.builder()
                .from("2026-04-01")
                .to("2026-04-30")
                .totalRevenue(new BigDecimal("20000000.00"))
                .totalCost(new BigDecimal("12000000.00"))
                .grossProfit(new BigDecimal("8000000.00"))
                .totalImportCount(5)
                .totalExportCount(10)
                .totalQuantityImported(100L)
                .totalQuantityExported(80L)
                .topSellingProducts(List.of(
                        TopSellingProductDto.builder().productId(1).productName("Chuột không dây").quantitySold(50L).totalRevenue(new BigDecimal("10000000.00")).build()
                ))
                .build();

        when(reportService.getBusinessReport(
                org.mockito.ArgumentMatchers.nullable(String.class),
                org.mockito.ArgumentMatchers.nullable(String.class),
                anyInt()
        )).thenReturn(mockResponse);

        mockMvc.perform(get("/api/report/business?from=2026-04-01&to=2026-04-30&top=5")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(20000000.00))
                .andExpect(jsonPath("$.totalCost").value(12000000.00))
                .andExpect(jsonPath("$.grossProfit").value(8000000.00))
                .andExpect(jsonPath("$.topSellingProducts.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/report/export/excel - Xuất file XLSX")
    void exportExcel_Xlsx_ReturnsFile() throws Exception {
        byte[] fakeXlsx = "Fake XLSX Content".getBytes();
        when(reportService.exportReport(
                org.mockito.ArgumentMatchers.nullable(String.class),
                org.mockito.ArgumentMatchers.nullable(String.class),
                org.mockito.ArgumentMatchers.nullable(String.class),
                org.mockito.ArgumentMatchers.nullable(String.class)
        )).thenReturn(fakeXlsx);

        mockMvc.perform(get("/api/report/export/excel?reportType=inventory&format=xlsx")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("attachment; filename=")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString(".xlsx")))
                .andExpect(content().bytes(fakeXlsx));
    }

    @Test
    @DisplayName("GET /api/report/stock - Quyền ThuKho bị từ chối 403 Forbidden")
    void getStockReport_ThuKho_Forbidden403() throws Exception {
        mockMvc.perform(get("/api/report/stock")
                        .header(HttpHeaders.AUTHORIZATION, thuKhoToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/report/stock - Không đăng nhập bị từ chối 401 Unauthorized")
    void getStockReport_Unauthenticated_Unauthorized401() throws Exception {
        mockMvc.perform(get("/api/report/stock"))
                .andExpect(status().isUnauthorized());
    }
}
