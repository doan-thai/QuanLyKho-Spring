package com.syncstock.supplier.integration;

import com.syncstock.supplier.config.RabbitMQConfig;
import com.syncstock.supplier.entity.SupplierInfo;
import com.syncstock.supplier.event.ImportCompletedEvent;
import com.syncstock.supplier.repository.ProcessedEventRepository;
import com.syncstock.supplier.repository.SupplierRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Test kiểm thử H-08: Consumer nhận trùng message (Idempotent Consumer)
 * sử dụng Testcontainers MySQL 8.0 và RabbitMQ 3.13-management.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class ConsumerDuplicateMessageIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.33")
            .withDatabaseName("supplier_db")
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
    private SupplierRepository supplierRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("H-08: Consumer nhận trùng cùng 1 ImportCompletedEvent 2 lần -> Chỉ xử lý 1 lần duy nhất (Idempotent)")
    void testConsumerDuplicateEvent_ShouldOnlyProcessOnce() {
        SupplierInfo supplier = supplierRepository.save(SupplierInfo.builder()
                .name("Nhà Cung Cấp Idempotent Test " + System.currentTimeMillis())
                .phone("0988776655")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .totalTransactions(1)
                .totalImportValue(new BigDecimal("10000000"))
                .reliabilityScore(new BigDecimal("30.00"))
                .build());

        int receiptId = 8888;
        BigDecimal importAmount = new BigDecimal("5000000");

        ImportCompletedEvent event = ImportCompletedEvent.builder()
                .receiptId(receiptId)
                .supplierId(supplier.getId())
                .totalImportAmount(importAmount)
                .importDate(LocalDateTime.now())
                .build();

        // Lần 1: Gửi event lên RabbitMQ
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.IMPORT_ROUTING_KEY, event);

        // Chờ consumer xử lý lần 1
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertTrue(processedEventRepository.existsById(receiptId), "ProcessedEvent phải được ghi nhận sau khi consumer xử lý");
            SupplierInfo s = supplierRepository.findById(supplier.getId()).orElseThrow();
            assertEquals(2, s.getTotalTransactions(), "totalTransactions phải tăng từ 1 lên 2");
            assertEquals(new BigDecimal("15000000.00"), s.getTotalImportValue());
        });

        // Lần 2: Gửi LẶP LẠI CHÍNH XÁC EVENT ĐÓ (nhận trùng do mạng/retry)
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.IMPORT_ROUTING_KEY, event);

        // Chờ thêm 3 giây để đảm bảo message trùng nếu bị xử lý sai sẽ gây lỗi
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ignored) {}

        // Kiểm tra lại DB: Số giao dịch và giá trị nhập KHÔNG ĐƯỢC PHÉP CỘNG THÊM
        SupplierInfo finalSupplier = supplierRepository.findById(supplier.getId()).orElseThrow();
        assertEquals(2, finalSupplier.getTotalTransactions(),
                "totalTransactions KHÔNG được phép cộng dồn thành 3 khi nhận message trùng");
        assertEquals(new BigDecimal("15000000.00"), finalSupplier.getTotalImportValue(),
                "totalImportValue KHÔNG được phép cộng dồn thành 20000000 khi nhận message trùng");
    }
}

