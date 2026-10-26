package com.syncstock.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroqChatResponse {
    private List<Choice> choices;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choice {
        private GroqChatRequest.Message message;
    }

    public String extractAnswer() {
        if (choices != null && !choices.isEmpty() && choices.getFirst().getMessage() != null) {
            return choices.getFirst().getMessage().getContent();
        }
        return null;
    }
}
