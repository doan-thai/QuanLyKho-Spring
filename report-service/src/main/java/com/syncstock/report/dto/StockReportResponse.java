package com.syncstock.report.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReportResponse {
    private int totalProducts;
    private long totalStock;
    private int lowStockCount;

    @JsonProperty("alertsCount")
    public int getAlertsCount() {
        return lowStockCount;
    }

    private List<StockReportItemDto> items;
}
