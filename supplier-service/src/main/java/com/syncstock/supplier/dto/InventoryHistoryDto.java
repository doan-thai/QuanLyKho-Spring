package com.syncstock.supplier.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryHistoryDto {
    private Integer id;
    private Integer productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private Integer supplierId;
    private String type;
    private String transactionType;
    private LocalDateTime date;
    private LocalDateTime transactionDate;
    private String note;
}
