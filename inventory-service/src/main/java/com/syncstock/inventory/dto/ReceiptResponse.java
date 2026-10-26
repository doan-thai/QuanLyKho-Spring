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
public class ReceiptResponse {
    private String message;
    private Integer receiptId;
    private String code;

    // Support both receiptId and ReceiptId, message and Message for maximum compatibility
    @JsonProperty("ReceiptId")
    public Integer getReceiptIdPascal() {
        return receiptId;
    }

    @JsonProperty("Message")
    public String getMessagePascal() {
        return message;
    }
}
