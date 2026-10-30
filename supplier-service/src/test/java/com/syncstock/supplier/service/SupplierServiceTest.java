package com.syncstock.supplier.service;

import com.syncstock.common.dto.PagedResult;
import com.syncstock.supplier.client.InventoryAnalyticsClient;
import com.syncstock.supplier.dto.InventoryHistoryDto;
import com.syncstock.supplier.dto.SupplierAnalyticsDto;
import com.syncstock.supplier.dto.SupplierUpsertRequest;
import com.syncstock.supplier.entity.SupplierInfo;
import com.syncstock.supplier.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private InventoryAnalyticsClient inventoryAnalyticsClient;

    @InjectMocks
    private SupplierService supplierService;

    private SupplierInfo supplier1;
    private SupplierInfo supplier2;

    @BeforeEach
    void setUp() {
        supplier1 = SupplierInfo.builder()
                .id(1)
                .name("Công ty ABC")
                .contactPerson("Nguyễn Văn A")
                .phone("0900000001")
                .email("abc@demo.com")
                .address("Hà Nội")
                .isActive(true)
                .createdAt(LocalDateTime.of(2026, 4, 1, 0, 0))
                .totalTransactions(0)
                .totalImportValue(BigDecimal.ZERO)
                .reliabilityScore(new BigDecimal("50.00"))
                .build();

        supplier2 = SupplierInfo.builder()
                .id(2)
                .name("Công ty XYZ")
                .contactPerson("Trần Thị B")
                .phone("0900000002")
                .email("xyz@demo.com")
                .address("TP HCM")
                .isActive(false)
                .createdAt(LocalDateTime.of(2026, 4, 1, 0, 0))
                .totalTransactions(0)
                .totalImportValue(BigDecimal.ZERO)
                .reliabilityScore(new BigDecimal("25.00"))
                .build();
    }

    @Test
    @DisplayName("getAll: Phân trang và trả về PagedResult với totalRecords")
    void getAll_ShouldReturnPagedResultWithTotalRecords() {
        when(supplierRepository.findAllByOrderByNameAsc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(supplier1, supplier2)));

        PagedResult<SupplierInfo> result = supplierService.getAll(null, 1, 10);

        assertNotNull(result);
        assertEquals(2, result.getTotalRecords());
        assertEquals(1, result.getPageNumber());
        assertEquals(10, result.getPageSize());
        assertEquals(2, result.getData().size());
        assertEquals("Công ty ABC", result.getData().get(0).getName());
    }

    @Test
    @DisplayName("create: Trùng tên nhà cung cấp -> Ném lỗi 400 Bad Request")
    void create_DuplicateName_ShouldThrow400() {
        SupplierUpsertRequest request = SupplierUpsertRequest.builder()
                .name("Công ty ABC")
                .phone("0912345678")
                .build();

        when(supplierRepository.existsByNameIgnoreCase("Công ty ABC")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> supplierService.create(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Tên nhà cung cấp đã tồn tại.", ex.getReason());
        verify(supplierRepository, never()).save(any());
    }

    @Test
    @DisplayName("create: Hợp lệ -> Lưu mới nhà cung cấp với các giá trị mặc định")
    void create_ValidRequest_ShouldSaveAndReturn() {
        SupplierUpsertRequest request = SupplierUpsertRequest.builder()
                .name("Công ty Tân Tạo")
                .contactPerson("Lê Văn C")
                .phone("0987654321")
                .email("tantao@demo.com")
                .address("Bình Tân, TP HCM")
                .isActive(true)
                .build();

        when(supplierRepository.existsByNameIgnoreCase("Công ty Tân Tạo")).thenReturn(false);
        when(supplierRepository.save(any(SupplierInfo.class))).thenAnswer(invocation -> {
            SupplierInfo s = invocation.getArgument(0);
            s.setId(10);
            return s;
        });

        SupplierInfo result = supplierService.create(request);

        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals("Công ty Tân Tạo", result.getName());
        assertEquals("0987654321", result.getPhone());
        assertEquals(0, result.getTotalTransactions());
        assertEquals(BigDecimal.ZERO, result.getTotalImportValue());
        verify(supplierRepository).save(any(SupplierInfo.class));
    }

    @Test
    @DisplayName("update: Không tìm thấy ID -> Ném lỗi 404 Not Found")
    void update_NotFound_ShouldThrow404() {
        SupplierUpsertRequest request = SupplierUpsertRequest.builder()
                .name("Công ty ABC Mới")
                .phone("0900000001")
                .build();

        when(supplierRepository.findById(999)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> supplierService.update(999, request));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Không tìm thấy nhà cung cấp.", ex.getReason());
    }

    @Test
    @DisplayName("update: Đổi tên thành tên của nhà cung cấp khác đã tồn tại -> Ném lỗi 400 Bad Request")
    void update_DuplicateName_ShouldThrow400() {
        SupplierUpsertRequest request = SupplierUpsertRequest.builder()
                .name("Công ty XYZ")
                .phone("0900000001")
                .build();

        when(supplierRepository.findById(1)).thenReturn(Optional.of(supplier1));
        when(supplierRepository.existsByNameIgnoreCaseAndIdNot("Công ty XYZ", 1)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> supplierService.update(1, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Tên nhà cung cấp đã tồn tại ở bản ghi khác.", ex.getReason());
    }

    @Test
    @DisplayName("delete: Không tìm thấy -> Ném 404; Tìm thấy -> Xóa thành công")
    void delete_ShouldHandleCorrectly() {
        when(supplierRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> supplierService.delete(999));

        when(supplierRepository.findById(1)).thenReturn(Optional.of(supplier1));
        supplierService.delete(1);
        verify(supplierRepository).delete(supplier1);
    }

    @Test
    @DisplayName("getAnalytics: Tính toán chính xác totalTransactions, totalImportValue và reliabilityScore (không saveAll vào DB)")
    void getAnalytics_ShouldCalculateReliabilityScoreProperly() {
        LocalDateTime recentDate = LocalDateTime.now().minusDays(5);
        List<InventoryHistoryDto> mockHistory = List.of(
                InventoryHistoryDto.builder()
                        .id(101)
                        .supplierId(1)
                        .type("IMPORT")
                        .quantity(10)
                        .unitPrice(BigDecimal.valueOf(10_000_000))
                        .totalAmount(BigDecimal.valueOf(100_000_000))
                        .transactionDate(recentDate)
                        .build(),
                InventoryHistoryDto.builder()
                        .id(102)
                        .supplierId(1)
                        .type("IMPORT")
                        .quantity(5)
                        .unitPrice(BigDecimal.valueOf(2_000_000))
                        .totalAmount(BigDecimal.valueOf(10_000_000))
                        .transactionDate(recentDate)
                        .build(),
                InventoryHistoryDto.builder()
                        .id(103)
                        .supplierId(1)
                        .type("EXPORT") // EXPORT should be filtered out
                        .quantity(2)
                        .unitPrice(BigDecimal.valueOf(5_000_000))
                        .totalAmount(BigDecimal.valueOf(10_000_000))
                        .transactionDate(recentDate)
                        .build()
        );

        when(inventoryAnalyticsClient.getHistory(1000, "Bearer token123")).thenReturn(mockHistory);
        when(supplierRepository.findAllByOrderByNameAsc()).thenReturn(List.of(supplier1, supplier2));

        List<SupplierAnalyticsDto> result = supplierService.getAnalytics(1000, "Bearer token123");

        assertNotNull(result);
        assertEquals(2, result.size());

        // Supplier 1: 2 import transactions, total = 110,000,000
        SupplierAnalyticsDto s1 = result.get(0);
        assertEquals(2, s1.getTotalTransactions());
        assertEquals(BigDecimal.valueOf(110_000_000), s1.getTotalImportValue());
        assertEquals(recentDate, s1.getLastTransactionAt());

        // Reliability score: 4 + 30 + 20 = 54.00
        assertEquals(new BigDecimal("54.00"), s1.getReliabilityScore());

        // Supplier 2: no transactions, inactive -> reliability = 25.00
        SupplierAnalyticsDto s2 = result.get(1);
        assertEquals(0, s2.getTotalTransactions());
        assertEquals(BigDecimal.ZERO, s2.getTotalImportValue());
        assertNull(s2.getLastTransactionAt());
        assertEquals(new BigDecimal("25.00"), s2.getReliabilityScore());

        // C-08: Ensure saveAll is NEVER called!
        verify(supplierRepository, never()).saveAll(any());
    }
}
