package com.syncstock.inventory.service;

import com.syncstock.inventory.dto.ExportRequest;
import com.syncstock.inventory.dto.ImportRequest;
import com.syncstock.inventory.dto.ReceiptResponse;
import com.syncstock.inventory.dto.StockItemRequest;
import com.syncstock.inventory.dto.StockItemResponse;
import com.syncstock.inventory.dto.StockTransactionResponse;
import com.syncstock.inventory.entity.ExportReceipt;
import com.syncstock.inventory.entity.ImportReceipt;
import com.syncstock.inventory.entity.Product;
import com.syncstock.inventory.entity.StockTransaction;
import com.syncstock.inventory.repository.ExportReceiptRepository;
import com.syncstock.inventory.repository.ImportReceiptRepository;
import com.syncstock.inventory.repository.ProductRepository;
import com.syncstock.inventory.repository.StockTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ImportReceiptRepository importReceiptRepository;

    @Mock
    private ExportReceiptRepository exportReceiptRepository;

    @Mock
    private StockTransactionRepository stockTransactionRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private InventoryService inventoryService;

    private Product normalProduct;
    private Product lowStockProduct;

    @BeforeEach
    void setUp() {
        normalProduct = Product.builder()
                .id(1)
                .name("Laptop Dell XPS 15")
                .category("Điện tử")
                .unit("Cái")
                .stock(50)
                .minStock(10)
                .build();

        lowStockProduct = Product.builder()
                .id(2)
                .name("Nồi chiên không dầu")
                .category("Gia dụng")
                .unit("Cái")
                .stock(3)
                .minStock(5)
                .build();
    }

    @Test
    @DisplayName("getStockOverview: Tính toán chính xác trường isLowStock (stock < minStock)")
    void getStockOverview_ShouldCalculateIsLowStock() {
        when(productRepository.findAllByOrderByIdAsc()).thenReturn(List.of(normalProduct, lowStockProduct));

        List<StockItemResponse> list = inventoryService.getStockOverview();

        assertNotNull(list);
        assertEquals(2, list.size());
        assertFalse(list.get(0).isLowStock()); // 50 >= 10
        assertTrue(list.get(1).isLowStock());  // 3 < 5
    }

    @Test
    @DisplayName("getTransactionHistory: Truy vấn lịch sử giao dịch kết hợp thông tin sản phẩm")
    void getTransactionHistory_ShouldReturnHistory() {
        StockTransactionResponse mockResp = new StockTransactionResponse(
                10,
                1,
                "Laptop Dell XPS 15",
                5,
                BigDecimal.valueOf(15000000),
                BigDecimal.valueOf(75000000),
                1,
                "IMPORT",
                "IMPORT",
                LocalDateTime.now(),
                LocalDateTime.now(),
                "Nhập theo phiếu 1"
        );

        when(stockTransactionRepository.findTransactionHistory(any(Pageable.class))).thenReturn(List.of(mockResp));

        List<StockTransactionResponse> result = inventoryService.getTransactionHistory(200);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("IMPORT", result.get(0).getType());
        assertEquals("IMPORT", result.get(0).getTransactionType());
        assertEquals("Laptop Dell XPS 15", result.get(0).getProductName());
        assertEquals(BigDecimal.valueOf(75000000), result.get(0).getTotalAmount());
    }

    @Test
    @DisplayName("importStock: Lưu phiếu nhập, tăng tồn kho sản phẩm và ghi nhận giao dịch IMPORT")
    void importStock_ShouldIncreaseStockAndRecordTransaction() {
        ImportRequest request = ImportRequest.builder()
                .code("PN-20261025-101")
                .note("Nhập đợt 1")
                .supplierId(1)
                .items(List.of(
                        StockItemRequest.builder()
                                .productId(1)
                                .quantity(10)
                                .price(BigDecimal.valueOf(20000000))
                                .build()
                ))
                .build();

        when(importReceiptRepository.save(any(ImportReceipt.class))).thenAnswer(invocation -> {
            ImportReceipt r = invocation.getArgument(0);
            r.setId(101);
            return r;
        });

        when(productRepository.findById(1)).thenReturn(Optional.of(normalProduct));

        ReceiptResponse response = inventoryService.importStock(request);

        assertNotNull(response);
        assertEquals("Nhập kho thành công!", response.getMessage());
        assertEquals(101, response.getReceiptId());
        assertEquals(60, normalProduct.getStock()); // 50 + 10 = 60

        verify(productRepository).save(normalProduct);
        verify(stockTransactionRepository).save(argThat(tx ->
                tx.getProductId() == 1 &&
                tx.getTransactionType().equals("IMPORT") &&
                tx.getQuantity() == 10 &&
                tx.getUnitPrice().compareTo(BigDecimal.valueOf(20000000)) == 0
        ));
    }

    @Test
    @DisplayName("exportStock: Đủ hàng -> trừ tồn kho, lưu phiếu xuất và ghi nhận giao dịch EXPORT")
    void exportStock_SufficientStock_ShouldDecreaseStockAndRecordTransaction() {
        ExportRequest request = ExportRequest.builder()
                .code("PX-20261025-202")
                .reason("Xuất bán lẻ")
                .items(List.of(
                        StockItemRequest.builder()
                                .productId(1)
                                .quantity(20)
                                .price(BigDecimal.valueOf(25000000))
                                .build()
                ))
                .build();

        when(exportReceiptRepository.save(any(ExportReceipt.class))).thenAnswer(invocation -> {
            ExportReceipt r = invocation.getArgument(0);
            r.setId(202);
            return r;
        });

        when(productRepository.findById(1)).thenReturn(Optional.of(normalProduct));

        ReceiptResponse response = inventoryService.exportStock(request);

        assertNotNull(response);
        assertEquals("Xuất kho thành công!", response.getMessage());
        assertEquals(202, response.getReceiptId());
        assertEquals(30, normalProduct.getStock()); // 50 - 20 = 30

        verify(productRepository).save(normalProduct);
        verify(stockTransactionRepository).save(argThat(tx ->
                tx.getProductId() == 1 &&
                tx.getTransactionType().equals("EXPORT") &&
                tx.getQuantity() == 20
        ));
    }

    @Test
    @DisplayName("exportStock: Không đủ hàng (stock < quantity) -> Ném lỗi 400 Bad Request và rollback")
    void exportStock_InsufficientStock_ShouldThrow400BadRequest() {
        ExportRequest request = ExportRequest.builder()
                .code("PX-20261025-303")
                .reason("Xuất số lượng lớn")
                .items(List.of(
                        StockItemRequest.builder()
                                .productId(1)
                                .quantity(100) // Tồn chỉ có 50
                                .price(BigDecimal.valueOf(25000000))
                                .build()
                ))
                .build();

        when(exportReceiptRepository.save(any(ExportReceipt.class))).thenAnswer(invocation -> {
            ExportReceipt r = invocation.getArgument(0);
            r.setId(303);
            return r;
        });

        when(productRepository.findById(1)).thenReturn(Optional.of(normalProduct));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> inventoryService.exportStock(request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Không đủ hàng: 'Laptop Dell XPS 15'. Tồn: 50, Yêu cầu: 100.", ex.getReason());

        // Đảm bảo không trừ stock và không ghi nhận giao dịch
        assertEquals(50, normalProduct.getStock());
        verify(stockTransactionRepository, never()).save(any(StockTransaction.class));
    }

    @Test
    @DisplayName("importStock: Có supplierId -> Phải publish ImportCompletedEvent lên RabbitMQ")
    void importStock_WithSupplierId_ShouldPublishEvent() {
        ImportRequest request = ImportRequest.builder()
                .code("PN-20261025-999")
                .note("Test RabbitMQ")
                .supplierId(1) // có supplierId hợp lệ
                .items(List.of(
                        StockItemRequest.builder()
                                .productId(1)
                                .quantity(5)
                                .price(java.math.BigDecimal.valueOf(10_000_000))
                                .build()
                ))
                .build();

        when(importReceiptRepository.save(any(ImportReceipt.class))).thenAnswer(invocation -> {
            ImportReceipt r = invocation.getArgument(0);
            r.setId(999);
            return r;
        });
        when(productRepository.findById(1)).thenReturn(java.util.Optional.of(normalProduct));

        inventoryService.importStock(request);

        // Phải gọi RabbitTemplate.convertAndSend đúng 1 lần với routing key đúng
        verify(rabbitTemplate).convertAndSend(
                eq(com.syncstock.inventory.config.RabbitMQConfig.EXCHANGE_NAME),
                eq(com.syncstock.inventory.config.RabbitMQConfig.IMPORT_ROUTING_KEY),
                any(com.syncstock.inventory.event.ImportCompletedEvent.class)
        );
    }

    @Test
    @DisplayName("importStock: Không có supplierId (null) -> Không publish event")
    void importStock_NoSupplierId_ShouldNotPublishEvent() {
        ImportRequest request = ImportRequest.builder()
                .code("PN-20261025-888")
                .supplierId(null) // không chọn NCC
                .items(List.of(
                        StockItemRequest.builder()
                                .productId(1)
                                .quantity(3)
                                .price(java.math.BigDecimal.valueOf(5_000_000))
                                .build()
                ))
                .build();

        when(importReceiptRepository.save(any(ImportReceipt.class))).thenAnswer(invocation -> {
            ImportReceipt r = invocation.getArgument(0);
            r.setId(888);
            return r;
        });
        when(productRepository.findById(1)).thenReturn(java.util.Optional.of(normalProduct));

        inventoryService.importStock(request);

        // Không được gọi RabbitTemplate khi không có supplierId
        verify(rabbitTemplate, never()).convertAndSend(
                any(String.class), any(String.class), any(Object.class));
    }
}
