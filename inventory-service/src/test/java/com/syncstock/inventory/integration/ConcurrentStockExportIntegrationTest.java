package com.syncstock.inventory.integration;

import com.syncstock.inventory.dto.ExportRequest;
import com.syncstock.inventory.dto.StockItemRequest;
import com.syncstock.inventory.entity.Product;
import com.syncstock.inventory.repository.ProductRepository;
import com.syncstock.inventory.service.InventoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Test kiểm thử tính năng Xuất kho đồng thời (Concurrent Stock-Out)
 * sử dụng Testcontainers MySQL 8.0 để kiểm tra Optimistic Locking (@Version) trên Product entity.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class ConcurrentStockExportIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.33")
            .withDatabaseName("warehouse_db")
            .withUsername("syncstock")
            .withPassword("syncstock123");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (mysql.isRunning()) {
            registry.add("spring.datasource.url", mysql::getJdbcUrl);
            registry.add("spring.datasource.username", mysql::getUsername);
            registry.add("spring.datasource.password", mysql::getPassword);
        }
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("Xuất kho đồng thời: 10 threads cùng xuất 1 sản phẩm -> Không bị Race Condition (âm kho)")
    void testConcurrentStockExport_ShouldPreventNegativeStockAndDataCorruption() throws InterruptedException {
        // Chuẩn bị 1 sản phẩm có tồn kho ban đầu = 10
        Product product = productRepository.save(Product.builder()
                .name("Sản phẩm Test Concurrent " + System.currentTimeMillis())
                .category("Điện tử")
                .unit("Cái")
                .stock(10)
                .minStock(2)
                .build());

        int threadCount = 10;
        int exportQuantityPerThread = 2; // Nếu cả 10 đều thành công thì cần 20, nhưng kho chỉ có 10
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    startLatch.await(); // Đồng loạt xuất phát cùng thời điểm
                    ExportRequest request = ExportRequest.builder()
                            .code("PX-CONCURRENT-" + index + "-" + System.currentTimeMillis())
                            .reason("Test xuất đồng thời")
                            .items(List.of(
                                    StockItemRequest.builder()
                                            .productId(product.getId())
                                            .quantity(exportQuantityPerThread)
                                            .price(BigDecimal.valueOf(100_000))
                                            .build()
                            ))
                            .build();

                    inventoryService.exportStock(request);
                    successCount.incrementAndGet();
                } catch (Throwable t) {
                    failureCount.incrementAndGet();
                    exceptions.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Bắn còi xuất phát cho tất cả threads
        doneLatch.await();
        executorService.shutdown();

        // Kiểm tra kết quả trong DB MySQL
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();

        // Tồn kho cuối cùng KHÔNG BAO GIỜ ĐƯỢC ÂM
        assertTrue(updatedProduct.getStock() >= 0, "Tồn kho không được phép bị âm khi xuất đồng thời");

        // Tồn kho cuối cùng phải khớp chính xác với số lượng xuất thành công
        int expectedStock = 10 - (successCount.get() * exportQuantityPerThread);
        assertEquals(expectedStock, updatedProduct.getStock(),
                "Tồn kho thực tế trong MySQL phải bằng [Tồn ban đầu - (Số lần xuất thành công * Số lượng)]");

        // Tổng số lượt hoàn thành (thành công + thất bại) phải đúng bằng số thread
        assertEquals(threadCount, successCount.get() + failureCount.get());

        // Do kho chỉ có 10 sản phẩm mà mỗi lần xuất 2, số lần thành công tối đa là 5
        assertTrue(successCount.get() <= 5, "Số lượt xuất thành công không thể vượt quá 5 khi tồn kho = 10");
    }
}

