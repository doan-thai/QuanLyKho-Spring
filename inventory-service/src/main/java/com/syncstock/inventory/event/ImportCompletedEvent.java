package com.syncstock.inventory.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event được publish lên RabbitMQ sau khi phiếu nhập kho hoàn thành thành công.
 * Consumer là supplier-service sẽ lắng nghe và cập nhật analytics NCC.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportCompletedEvent {

    /** ID phiếu nhập kho vừa được lưu */
    private Integer receiptId;

    /** ID nhà cung cấp (có thể null nếu không chọn NCC) */
    private Integer supplierId;

    /** Tổng giá trị nhập (tổng unitPrice * quantity của tất cả items) */
    private BigDecimal totalImportAmount;

    /** Ngày giờ phiếu nhập được tạo */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime importDate;
}
