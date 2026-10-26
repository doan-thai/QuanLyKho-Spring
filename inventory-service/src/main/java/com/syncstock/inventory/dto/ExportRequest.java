package com.syncstock.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportRequest {

    @Size(max = 50, message = "Mã phiếu không được vượt quá 50 ký tự")
    private String code;

    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    private String note;

    @Size(max = 500, message = "Lý do không được vượt quá 500 ký tự")
    private String reason;

    @NotEmpty(message = "Danh sách sản phẩm xuất kho không được để trống")
    @Valid
    private List<StockItemRequest> items;
}
