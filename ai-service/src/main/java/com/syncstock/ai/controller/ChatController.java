package com.syncstock.ai.controller;

import com.syncstock.ai.dto.ChatRequest;
import com.syncstock.ai.dto.ChatResponse;
import com.syncstock.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final AiService aiService;

    @PostMapping({"", "/ask"})
    public ResponseEntity<ChatResponse> chat(@RequestBody(required = false) ChatRequest request) {
        ChatResponse response = aiService.chat(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/summary")
    public ResponseEntity<ChatResponse> summary() {
        ChatResponse response = aiService.generateSummary();
        return ResponseEntity.ok(response);
    }
}
