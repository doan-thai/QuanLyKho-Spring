package com.syncstock.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType; // "IMPORT" | "EXPORT"

    @Column(nullable = false)
    private Integer quantity;

    @Builder.Default
    @Column(name = "unit_price", precision = 18, scale = 2, nullable = false)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "supplier_id")
    private Integer supplierId;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(length = 500)
    private String note;
}
