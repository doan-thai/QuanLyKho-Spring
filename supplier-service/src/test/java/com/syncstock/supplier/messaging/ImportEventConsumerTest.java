package com.syncstock.supplier.messaging;

import com.syncstock.supplier.entity.SupplierInfo;
import com.syncstock.supplier.event.ImportCompletedEvent;
import com.syncstock.supplier.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportEventConsumerTest {

    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private ImportEventConsumer consumer;

    private SupplierInfo supplier;

    @BeforeEach
    void setUp() {
        supplier = SupplierInfo.builder()
                .id(1)
                .name("Công ty ABC")
                .phone("0900000001")
                .isActive(true)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .totalTransactions(2)
                .totalImportValue(new BigDecimal("50000000"))
                .reliabilityScore(new BigDecimal("30.00"))
                .lastTransactionAt(null)
                .build();
    }

    @Test
    @DisplayName("handleImportCompleted: Event hợp lệ -> cộng dồn transactions, value và tính lại score")
    void handleImportCompleted_ValidEvent_ShouldUpdateSupplier() {
        ImportCompletedEvent event = ImportCompletedEvent.builder()
                .receiptId(100)
                .supplierId(1)
                .totalImportAmount(new BigDecimal("50000000")) // +50M
                .importDate(LocalDateTime.now().minusDays(5))
                .build();

        when(supplierRepository.findById(1)).thenReturn(Optional.of(supplier));
        when(supplierRepository.save(any(SupplierInfo.class))).thenAnswer(i -> i.getArgument(0));

        consumer.handleImportCompleted(event);

        ArgumentCaptor<SupplierInfo> captor = ArgumentCaptor.forClass(SupplierInfo.class);
        verify(supplierRepository).save(captor.capture());

        SupplierInfo saved = captor.getValue();
        assertEquals(3, saved.getTotalTransactions());   // 2 + 1
        assertEquals(new BigDecimal("100000000"), saved.getTotalImportValue()); // 50M + 50M
        assertNotNull(saved.getLastTransactionAt());
        // score phải > 0
        assertTrue(saved.getReliabilityScore().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("handleImportCompleted: supplierId không tồn tại -> không lưu gì cả")
    void handleImportCompleted_SupplierNotFound_ShouldSkip() {
        ImportCompletedEvent event = ImportCompletedEvent.builder()
                .receiptId(101)
                .supplierId(999)
                .totalImportAmount(BigDecimal.valueOf(10_000_000))
                .importDate(LocalDateTime.now())
                .build();

        when(supplierRepository.findById(999)).thenReturn(Optional.empty());

        consumer.handleImportCompleted(event);

        verify(supplierRepository, never()).save(any());
    }

    @Test
    @DisplayName("handleImportCompleted: Event null hoặc supplierId null -> bỏ qua")
    void handleImportCompleted_NullEvent_ShouldSkip() {
        consumer.handleImportCompleted(null);
        verify(supplierRepository, never()).findById(any());
        verify(supplierRepository, never()).save(any());

        ImportCompletedEvent noSupplier = ImportCompletedEvent.builder()
                .receiptId(102)
                .supplierId(null)
                .build();
        consumer.handleImportCompleted(noSupplier);
        verify(supplierRepository, never()).findById(any());
    }

    @Test
    @DisplayName("handleImportCompleted: Nhà cung cấp không hoạt động -> score bị nhân 0.5")
    void handleImportCompleted_InactiveSupplier_ShouldHalveScore() {
        supplier.setIsActive(false);
        supplier.setTotalTransactions(0);
        supplier.setTotalImportValue(BigDecimal.ZERO);

        ImportCompletedEvent event = ImportCompletedEvent.builder()
                .receiptId(103)
                .supplierId(1)
                .totalImportAmount(BigDecimal.valueOf(200_000_000)) // 200M -> valueScore = 30
                .importDate(LocalDateTime.now().minusDays(5))      // recencyScore = 20
                .build();

        when(supplierRepository.findById(1)).thenReturn(Optional.of(supplier));
        when(supplierRepository.save(any(SupplierInfo.class))).thenAnswer(i -> i.getArgument(0));

        consumer.handleImportCompleted(event);

        ArgumentCaptor<SupplierInfo> captor = ArgumentCaptor.forClass(SupplierInfo.class);
        verify(supplierRepository).save(captor.capture());

        // count=1 -> activity=2, value=200M->30, recency<=30->20 => score=(2+30+20)*0.5=26.0
        BigDecimal score = captor.getValue().getReliabilityScore();
        assertEquals(new BigDecimal("26.00"), score);
    }
}
