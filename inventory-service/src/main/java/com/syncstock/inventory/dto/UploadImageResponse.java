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
public class UploadImageResponse {
    private String url;
    private String message;

    @JsonProperty("Url")
    public String getUrlPascal() {
        return url;
    }

    @JsonProperty("Message")
    public String getMessagePascal() {
        return message;
    }
}
