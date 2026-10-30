package com.syncstock.inventory.integration;

import com.syncstock.inventory.config.RabbitMQConfig;
import com.syncstock.inventory.dto.ImportRequest;
import com.syncstock.inventory.dto.ReceiptResponse;
import com.syncstock.inventory.dto.StockItemRequest;
import com.syncstock.inventory.entity.Product;
import com.syncstock.inventory.event.ImportCompletedEvent;
import com.syncstock.inventory.repository.ProductRepository;
import com.syncstock.inventory.service.InventoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Test kiểm thử C-06: Publish sự kiện sau Commit (@TransactionalEventListener AFTER_COMMIT)
 * sử dụng Testcontainers MySQL 8.0 và RabbitMQ 3.13-management.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class AfterCommitEventPublishingIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.33")
            .withDatabaseName("warehouse_db")
            .withUsername("syncstock")
            .withPassword("syncstock123");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (mysql.isRunning()) {
            registry.add("spring.datasource.url", mysql::getJdbcUrl);
            registry.add("spring.datasource.username", mysql::getUsername);
            registry.add("spring.datasource.password", mysql::getPassword);
        }
        if (rabbitmq.isRunning()) {
            registry.add("spring.rabbitmq.host", rabbitmq::getHost);
            registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
            registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
            registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);
        }
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("C-06: importStock commit thành công -> Publish ImportCompletedEvent lên RabbitMQ sau commit")
    void importStock_CommitSuccess_ShouldPublishEventToRabbitMQ() {
        Product product = productRepository.save(Product.builder()
                .name("Sản phẩm Nhập Kho Test " + System.currentTimeMillis())
                .category("Gia dụng")
                .unit("Bộ")
                .stock(5)
                .minStock(2)
                .build());

        ImportRequest request = ImportRequest.builder()
                .code("PN-AFTER-COMMIT-" + System.currentTimeMillis())
                .supplierId(42)
                .items(List.of(
                        StockItemRequest.builder()
                                .productId(product.getId())
                                .quantity(10)
                                .price(BigDecimal.valueOf(250_000))
                                .build()
                ))
                .build();

        ReceiptResponse response = inventoryService.importStock(request);
        assertNotNull(response);

        // Đọc message từ RabbitMQ queue để xác thực event đã được publish sau commit
        ImportCompletedEvent event = rabbitTemplate.receiveAndConvert(
                RabbitMQConfig.IMPORT_QUEUE,
                5000,
                new ParameterizedTypeReference<ImportCompletedEvent>() {}
        );

        assertNotNull(event, "RabbitMQ queue phải nhận được ImportCompletedEvent sau khi transaction commit");
        assertEquals(response.getReceiptId(), event.getReceiptId());
        assertEquals(42, event.getSupplierId());
        assertEquals(new BigDecimal("2500000"), event.getTotalImportAmount());
    }

    @Test
    @DisplayName("C-06: Transaction bị Rollback -> KHÔNG publish bất kỳ sự kiện nào lên RabbitMQ")
    void importStock_Rollback_ShouldNotPublishEventToRabbitMQ() {
        Product product = productRepository.save(Product.builder()
                .name("Sản phẩm Rollback Test " + System.currentTimeMillis())
                .category("Gia dụng")
                .unit("Bộ")
                .stock(5)
                .minStock(2)
                .build());

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        assertThrows(RuntimeException.class, () -> {
            txTemplate.execute(status -> {
                ImportRequest request = ImportRequest.builder()
                        .code("PN-ROLLBACK-" + System.currentTimeMillis())
                        .supplierId(99)
                        .items(List.of(
                                StockItemRequest.builder()
                                        .productId(product.getId())
                                        .quantity(5)
                                        .price(BigDecimal.valueOf(100_000))
                                        .build()
                        ))
                        .build();

                inventoryService.importStock(request);

                // Giả lập lỗi ném ra trước khi commit -> Transaction Rollback
                throw new RuntimeException("Cố tình ném lỗi để kiểm tra Rollback không gửi event");
            });
        });

        // Kiểm tra RabbitMQ queue: Phải KHÔNG có message nào từ transaction đã rollback
        ImportCompletedEvent event = rabbitTemplate.receiveAndConvert(
                RabbitMQConfig.IMPORT_QUEUE,
                2000,
                new ParameterizedTypeReference<ImportCompletedEvent>() {}
        );

        assertNull(event, "Khi transaction rollback, @TransactionalEventListener(AFTER_COMMIT) không được phép gửi event lên RabbitMQ");
    }
}

