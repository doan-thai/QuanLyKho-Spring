package com.syncstock.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private String title;
    private String message;
    private int status;
    private LocalDateTime timestamp;

    public static ErrorResponse of(int status, String message) {
        return ErrorResponse.builder()
                .title(message)
                .message(message)
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
