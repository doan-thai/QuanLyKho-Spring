package com.syncstock.inventory.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockItemResponse {
    private Integer id;
    private String name;
    private String category;
    private String unit;
    private Integer stock;
    private Integer minStock;
    private String imageUrl;

    @JsonProperty("isLowStock")
    private boolean isLowStock;

    public boolean isLowStock() {
        return isLowStock;
    }

    public void setLowStock(boolean isLowStock) {
        this.isLowStock = isLowStock;
    }

    @JsonProperty("IsLowStock")
    public boolean getIsLowStock() {
        return isLowStock;
    }
}
