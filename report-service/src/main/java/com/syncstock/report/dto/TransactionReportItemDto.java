package com.syncstock.report.dto;

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
public class TransactionReportItemDto {
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

    public String getType() {
        return type != null ? type : transactionType;
    }

    public String getTransactionType() {
        return transactionType != null ? transactionType : type;
    }

    public LocalDateTime getDate() {
        return date != null ? date : transactionDate;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate != null ? transactionDate : date;
    }
}
