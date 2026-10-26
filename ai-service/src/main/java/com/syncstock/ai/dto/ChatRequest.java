package com.syncstock.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String message;
    private String prompt;
    private String question;

    public String getEffectiveMessage() {
        if (message != null && !message.isBlank()) {
            return message.trim();
        }
        if (prompt != null && !prompt.isBlank()) {
            return prompt.trim();
        }
        if (question != null && !question.isBlank()) {
            return question.trim();
        }
        return "";
    }
}
