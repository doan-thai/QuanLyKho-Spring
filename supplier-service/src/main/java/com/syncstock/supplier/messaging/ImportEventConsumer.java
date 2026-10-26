package com.syncstock.supplier.messaging;

import com.syncstock.supplier.config.RabbitMQConfig;
import com.syncstock.supplier.entity.SupplierInfo;
import com.syncstock.supplier.event.ImportCompletedEvent;
import com.syncstock.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Consumer nhận sự kiện ImportCompletedEvent từ inventory-service qua RabbitMQ.
 * Cập nhật analytics của nhà cung cấp trong supplier_db một cách bất đồng bộ,
 * không cần người dùng phải gọi API /api/supplier/analytics.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImportEventConsumer {

    private final SupplierRepository supplierRepository;

    @RabbitListener(queues = RabbitMQConfig.IMPORT_QUEUE)
    @Transactional
    public void handleImportCompleted(ImportCompletedEvent event) {
        if (event == null || event.getSupplierId() == null) {
            log.warn("[RabbitMQ] Received null or incomplete ImportCompletedEvent, skipping.");
            return;
        }

        log.info("[RabbitMQ] Received ImportCompletedEvent: receiptId={}, supplierId={}, amount={}",
                event.getReceiptId(), event.getSupplierId(), event.getTotalImportAmount());

        Optional<SupplierInfo> optSupplier = supplierRepository.findById(event.getSupplierId());
        if (optSupplier.isEmpty()) {
            log.warn("[RabbitMQ] Supplier #{} not found in supplier_db, skipping analytics update.",
                    event.getSupplierId());
            return;
        }

        SupplierInfo supplier = optSupplier.get();

        // Cộng dồn số giao dịch
        int newCount = supplier.getTotalTransactions() + 1;

        // Cộng dồn tổng giá trị nhập
        BigDecimal importAmount = event.getTotalImportAmount() != null
                ? event.getTotalImportAmount()
                : BigDecimal.ZERO;
        BigDecimal newTotalValue = supplier.getTotalImportValue().add(importAmount);

        // Cập nhật ngày giao dịch gần nhất
        LocalDateTime lastTx = event.getImportDate() != null
                ? event.getImportDate()
                : LocalDateTime.now();

        // Tính lại reliabilityScore
        BigDecimal newScore = calculateReliabilityScore(supplier, newCount, newTotalValue, lastTx);

        supplier.setTotalTransactions(newCount);
        supplier.setTotalImportValue(newTotalValue);
        supplier.setLastTransactionAt(lastTx);
        supplier.setReliabilityScore(newScore);

        supplierRepository.save(supplier);

        log.info("[RabbitMQ] Updated supplier #{} ({}): transactions={}, totalValue={}, score={}",
                supplier.getId(), supplier.getName(), newCount, newTotalValue, newScore);
    }

    /**
     * Tính điểm độ tin cậy theo cùng thuật toán như SupplierService.getAnalytics().
     */
    private BigDecimal calculateReliabilityScore(SupplierInfo supplier, int count,
                                                  BigDecimal totalValue, LocalDateTime lastDate) {
        double ageDays = 0.0;
        if (lastDate != null) {
            long seconds = Duration.between(lastDate, LocalDateTime.now()).getSeconds();
            ageDays = Math.max(0.0, seconds / 86400.0);
        }

        double activityScore = Math.min(50.0, count * 2.0);
        double totalValDouble = totalValue != null ? totalValue.doubleValue() : 0.0;
        double valueScore    = Math.min(30.0, (totalValDouble / 100_000_000.0) * 30.0);
        double recencyScore  = ageDays <= 30.0 ? 20.0 : (ageDays <= 90.0 ? 12.0 : 6.0);

        double score = activityScore + valueScore + recencyScore;
        if (Boolean.FALSE.equals(supplier.getIsActive())) {
            score *= 0.5;
        }

        double clamped = Math.clamp(score, 0.0, 100.0);
        return BigDecimal.valueOf(clamped).setScale(2, RoundingMode.HALF_UP);
    }
}
