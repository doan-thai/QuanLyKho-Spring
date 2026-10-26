package com.syncstock.inventory.dto;

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
public class StockTransactionResponse {
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

    public StockTransactionResponse(
            Integer id,
            Integer productId,
            String productName,
            Integer quantity,
            BigDecimal unitPrice,
            Integer supplierId,
            String transactionType,
            LocalDateTime transactionDate,
            String note
    ) {
        this.id = id;
        this.productId = productId;
        this.productName = productName != null ? productName : ("Sản phẩm #" + productId);
        this.quantity = quantity != null ? quantity : 0;
        this.unitPrice = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        this.totalAmount = this.unitPrice.multiply(BigDecimal.valueOf(this.quantity));
        this.supplierId = supplierId;
        this.type = transactionType;
        this.transactionType = transactionType;
        this.date = transactionDate;
        this.transactionDate = transactionDate;
        this.note = note;
    }
}
