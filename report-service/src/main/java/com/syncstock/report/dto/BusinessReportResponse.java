package com.syncstock.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessReportResponse {
    private String from;
    private String to;
    private BigDecimal totalRevenue;
    private BigDecimal totalCost;
    private BigDecimal grossProfit;
    private int totalImportCount;
    private int totalExportCount;
    private long totalQuantityImported;
    private long totalQuantityExported;
    private List<TopSellingProductDto> topSellingProducts;
}
