package com.syncstock.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpsertRequest {
    private Integer id;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 255, message = "Tên sản phẩm không được vượt quá 255 ký tự")
    private String name;

    @NotBlank(message = "Danh mục không được để trống")
    @Size(max = 100, message = "Danh mục không được vượt quá 100 ký tự")
    private String category;

    @NotBlank(message = "Đơn vị tính không được để trống")
    @Size(max = 50, message = "Đơn vị tính không được vượt quá 50 ký tự")
    private String unit;

    @Min(value = 0, message = "Số lượng tồn kho không được là số âm")
    @Builder.Default
    private Integer stock = 0;

    @Min(value = 0, message = "Mức tồn kho tối thiểu không hợp lệ")
    @Builder.Default
    private Integer minStock = 10;

    @Size(max = 500, message = "Đường dẫn ảnh không được vượt quá 500 ký tự")
    private String imageUrl;

    private BigDecimal price;
}
