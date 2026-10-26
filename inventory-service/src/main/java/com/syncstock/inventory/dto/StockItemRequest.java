package com.syncstock.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockItemRequest {

    @NotNull(message = "Mã sản phẩm (productId) là bắt buộc")
    private Integer productId;

    @NotNull(message = "Số lượng (quantity) là bắt buộc")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantity;

    private BigDecimal price;
}
