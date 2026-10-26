package com.syncstock.supplier.config;

import com.syncstock.supplier.entity.SupplierInfo;
import com.syncstock.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final SupplierRepository supplierRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (supplierRepository.count() > 0) {
            log.info("Suppliers already exist, skipping seeding.");
            return;
        }

        List<SupplierInfo> initialSuppliers = List.of(
                SupplierInfo.builder()
                        .name("Công ty ABC")
                        .contactPerson("Nguyễn Văn A")
                        .phone("0900000001")
                        .email("abc@demo.com")
                        .address("Hà Nội")
                        .isActive(true)
                        .createdAt(LocalDateTime.of(2026, 4, 1, 0, 0, 0))
                        .totalTransactions(0)
                        .totalImportValue(BigDecimal.ZERO)
                        .reliabilityScore(new BigDecimal("50.00"))
                        .lastTransactionAt(null)
                        .build(),
                SupplierInfo.builder()
                        .name("Công ty XYZ")
                        .contactPerson("Trần Thị B")
                        .phone("0900000002")
                        .email("xyz@demo.com")
                        .address("TP HCM")
                        .isActive(true)
                        .createdAt(LocalDateTime.of(2026, 4, 1, 0, 0, 0))
                        .totalTransactions(0)
                        .totalImportValue(BigDecimal.ZERO)
                        .reliabilityScore(new BigDecimal("50.00"))
                        .lastTransactionAt(null)
                        .build()
        );

        supplierRepository.saveAll(initialSuppliers);
        log.info("Successfully seeded {} sample suppliers (Công ty ABC, Công ty XYZ).", initialSuppliers.size());
    }
}
