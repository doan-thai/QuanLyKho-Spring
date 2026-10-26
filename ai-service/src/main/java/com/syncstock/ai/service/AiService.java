package com.syncstock.ai.service;

import com.syncstock.ai.client.InventoryClient;
import com.syncstock.ai.dto.ChatRequest;
import com.syncstock.ai.dto.ChatResponse;
import com.syncstock.ai.dto.GroqChatRequest;
import com.syncstock.ai.dto.GroqChatResponse;
import com.syncstock.ai.dto.StockItemDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AiService {

    private final InventoryClient inventoryClient;
    private final RestClient groqRestClient;
    private final String apiKey;
    private final String model;

    public AiService(
            InventoryClient inventoryClient,
            @Value("${groq.api-key:}") String apiKey,
            @Value("${groq.model:llama-3.1-8b-instant}") String model,
            @Value("${groq.base-url:https://api.groq.com/openai/v1}") String baseUrl
    ) {
        this.inventoryClient = inventoryClient;
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = (model != null && !model.isBlank()) ? model.trim() : "llama-3.1-8b-instant";

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(15));
        factory.setReadTimeout(Duration.ofSeconds(30));

        this.groqRestClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public ChatResponse chat(ChatRequest request) {
        String userQuestion = request != null ? request.getEffectiveMessage() : "";
        if (userQuestion.isBlank()) {
            return new ChatResponse("Xin chào! Tôi là SyncStock AI. Tôi có thể hỗ trợ gì cho sếp về tình hình tồn kho hôm nay?");
        }

        List<StockItemDto> stockList = inventoryClient.getStock();
        String inventoryContext = buildInventoryContext(stockList);
        String systemPrompt = buildSystemPrompt(inventoryContext);

        if (!apiKey.isBlank()) {
            try {
                String groqAnswer = callGroq(systemPrompt, userQuestion);
                if (groqAnswer != null && !groqAnswer.isBlank()) {
                    return new ChatResponse(groqAnswer.trim());
                }
            } catch (Exception ex) {
                log.warn("Groq API call failed: {}. Falling back to rule-based answer.", ex.getMessage());
            }
        }

        // Fallback thông minh dựa trên dữ liệu kho thực tế khi chưa có API key hoặc lỗi mạng
        String fallbackAnswer = generateRuleBasedAnswer(userQuestion, stockList);
        return new ChatResponse(fallbackAnswer);
    }

    public ChatResponse generateSummary() {
        List<StockItemDto> stockList = inventoryClient.getStock();
        String inventoryContext = buildInventoryContext(stockList);
        String systemPrompt = buildSystemPrompt(inventoryContext);
        String summaryQuestion = "Hãy tóm tắt tình hình kho hiện tại thành 5 đến 7 dòng ngắn gọn, súc tích, bao gồm: tổng số mặt hàng, tổng lượng tồn, các mặt hàng sắp hết cần chú ý và khuyến nghị nhập kho.";

        if (!apiKey.isBlank()) {
            try {
                String groqAnswer = callGroq(systemPrompt, summaryQuestion);
                if (groqAnswer != null && !groqAnswer.isBlank()) {
                    return new ChatResponse(groqAnswer.trim());
                }
            } catch (Exception ex) {
                log.warn("Groq API call for summary failed: {}. Falling back to rule-based summary.", ex.getMessage());
            }
        }

        String fallbackSummary = generateRuleBasedSummary(stockList);
        return new ChatResponse(fallbackSummary);
    }

    private String callGroq(String systemPrompt, String userMessage) {
        GroqChatRequest groqRequest = GroqChatRequest.builder()
                .model(this.model)
                .temperature(0.4)
                .maxTokens(1024)
                .messages(List.of(
                        GroqChatRequest.Message.builder().role("system").content(systemPrompt).build(),
                        GroqChatRequest.Message.builder().role("user").content(userMessage).build()
                ))
                .build();

        GroqChatResponse response = groqRestClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(groqRequest)
                .retrieve()
                .body(GroqChatResponse.class);

        return response != null ? response.extractAnswer() : null;
    }

    private String buildSystemPrompt(String inventoryContext) {
        return """
                Bạn là trợ lý AI thông minh của hệ thống quản lý kho SyncStock (tên là SyncStock AI).
                Dưới đây là thông tin tồn kho thực tế được cập nhật tại thời điểm hiện tại:
                === DỮ LIỆU TỒN KHO THỰC TẾ ===
                %s
                ===============================
                Quy tắc trả lời:
                - Bạn trả lời hoàn toàn bằng tiếng Việt lịch sự, thân thiện và chuyên nghiệp (xưng "tôi", gọi người dùng là "bạn" hoặc "sếp").
                - Luôn dựa vào dữ liệu tồn kho thực tế ở trên để trả lời chính xác số lượng tồn, danh mục, và trạng thái cảnh báo thiếu hàng.
                - Nếu người dùng hỏi sản phẩm không có trong danh sách trên, hãy thông báo lịch sự là hệ thống chưa ghi nhận sản phẩm đó.
                - Giữ câu trả lời rõ ràng, mạch lạc, dễ đọc trên giao diện dashboard.
                """.formatted(inventoryContext);
    }

    private String buildInventoryContext(List<StockItemDto> stockList) {
        if (stockList == null || stockList.isEmpty()) {
            return "Hiện tại kho chưa có sản phẩm nào.";
        }

        long totalStock = stockList.stream().mapToLong(i -> i.getStock() != null ? i.getStock() : 0).sum();
        List<StockItemDto> lowStockItems = stockList.stream().filter(StockItemDto::isLowStock).toList();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("- Tổng số loại mặt hàng: %d\n", stockList.size()));
        sb.append(String.format("- Tổng số lượng hàng hóa trong kho: %d\n", totalStock));
        sb.append(String.format("- Số mặt hàng sắp hết/cảnh báo thiếu: %d\n", lowStockItems.size()));

        if (!lowStockItems.isEmpty()) {
            sb.append("- Danh sách mặt hàng thiếu hàng:\n");
            for (StockItemDto item : lowStockItems) {
                sb.append(String.format("  + %s (Tồn: %d %s, Tối thiểu: %d %s)\n",
                        item.getName(), item.getStock(), item.getUnit() != null ? item.getUnit() : "",
                        item.getMinStock(), item.getUnit() != null ? item.getUnit() : ""));
            }
        }

        sb.append("- Chi tiết toàn bộ tồn kho:\n");
        int index = 1;
        for (StockItemDto item : stockList) {
            sb.append(String.format("  %d. %s | Danh mục: %s | Tồn: %d %s | Tối thiểu: %d %s%s\n",
                    index++,
                    item.getName(),
                    item.getCategory(),
                    item.getStock(),
                    item.getUnit() != null ? item.getUnit() : "",
                    item.getMinStock(),
                    item.getUnit() != null ? item.getUnit() : "",
                    item.isLowStock() ? " [CẢNH BÁO: THIẾU HÀNG]" : ""
            ));
        }

        return sb.toString();
    }

    private String generateRuleBasedAnswer(String userQuestion, List<StockItemDto> stockList) {
        String lowerQuestion = userQuestion.toLowerCase();

        if (stockList == null || stockList.isEmpty()) {
            return "Hệ thống kho hiện tại chưa có dữ liệu sản phẩm nào.";
        }

        // 1. Hỏi về mặt hàng thiếu / hết hàng
        if (lowerQuestion.contains("thiếu") || lowerQuestion.contains("hết") || lowerQuestion.contains("cảnh báo") || lowerQuestion.contains("low")) {
            List<StockItemDto> lowItems = stockList.stream().filter(StockItemDto::isLowStock).toList();
            if (lowItems.isEmpty()) {
                return "Hiện tại tất cả các mặt hàng trong kho đều đang ở mức an toàn (không có sản phẩm nào bị thiếu hàng).";
            }
            StringBuilder sb = new StringBuilder("Hiện có " + lowItems.size() + " mặt hàng đang ở mức cảnh báo thiếu hàng:\n");
            for (StockItemDto item : lowItems) {
                sb.append(String.format("- %s (Tồn: %d %s, Mức tối thiểu: %d %s)\n",
                        item.getName(), item.getStock(), item.getUnit() != null ? item.getUnit() : "",
                        item.getMinStock(), item.getUnit() != null ? item.getUnit() : ""));
            }
            sb.append("Sếp nên xem xét tạo phiếu nhập bổ sung cho các mặt hàng này nhé!");
            return sb.toString();
        }

        // 2. Hỏi về tổng tồn kho / thống kê
        if (lowerQuestion.contains("tổng") || lowerQuestion.contains("bao nhiêu") || lowerQuestion.contains("thống kê")) {
            long totalStock = stockList.stream().mapToLong(i -> i.getStock() != null ? i.getStock() : 0).sum();
            int lowCount = (int) stockList.stream().filter(StockItemDto::isLowStock).count();
            return String.format("Kho hiện đang quản lý %d loại mặt hàng với tổng tồn kho là %d đơn vị sản phẩm. Trong đó có %d mặt hàng đang ở mức cảnh báo thiếu hàng.",
                    stockList.size(), totalStock, lowCount);
        }

        // 3. Tìm sản phẩm cụ thể theo từ khóa
        List<StockItemDto> matched = new ArrayList<>();
        for (StockItemDto item : stockList) {
            if (item.getName() != null && lowerQuestion.contains(item.getName().toLowerCase())) {
                matched.add(item);
            }
        }

        if (!matched.isEmpty()) {
            StringBuilder sb = new StringBuilder("Dưới đây là thông tin các mặt hàng sếp đang tìm:\n");
            for (StockItemDto item : matched) {
                sb.append(String.format("- %s: Tồn %d %s (Mức tối thiểu: %d %s)%s\n",
                        item.getName(), item.getStock(), item.getUnit() != null ? item.getUnit() : "",
                        item.getMinStock(), item.getUnit() != null ? item.getUnit() : "",
                        item.isLowStock() ? " -> CẢNH BÁO THIẾU HÀNG!" : " -> Đủ hàng"));
            }
            return sb.toString();
        }

        return "Tôi đã nhận được câu hỏi của sếp: \"" + userQuestion + "\". Hiện tại kho có " + stockList.size() + " loại mặt hàng. Sếp có thể hỏi cụ thể về tên sản phẩm, danh mục, hoặc các mặt hàng sắp hết để tôi hỗ trợ nhanh nhất nhé!";
    }

    private String generateRuleBasedSummary(List<StockItemDto> stockList) {
        if (stockList == null || stockList.isEmpty()) {
            return "Kho hàng hiện chưa có dữ liệu sản phẩm nào được ghi nhận.";
        }

        long totalStock = stockList.stream().mapToLong(i -> i.getStock() != null ? i.getStock() : 0).sum();
        List<StockItemDto> lowItems = stockList.stream().filter(StockItemDto::isLowStock).toList();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("1. Tổng số loại mặt hàng đang quản lý: %d sản phẩm.\n", stockList.size()));
        sb.append(String.format("2. Tổng sản lượng hàng hóa lưu kho hiện tại: %d đơn vị.\n", totalStock));
        sb.append(String.format("3. Số mặt hàng đang chạm ngưỡng cảnh báo thiếu: %d sản phẩm.\n", lowItems.size()));

        if (!lowItems.isEmpty()) {
            String sampleNames = lowItems.stream().limit(3).map(StockItemDto::getName).reduce((a, b) -> a + ", " + b).orElse("");
            sb.append(String.format("4. Mặt hàng cần lưu ý nhập bổ sung: %s%s.\n", sampleNames, lowItems.size() > 3 ? " và các mặt hàng khác" : ""));
        } else {
            sb.append("4. Tình trạng dự trữ: Rất an toàn, chưa có mặt hàng nào dưới mức định mức.\n");
        }

        sb.append("5. Khuyến nghị điều phối: Tiếp tục theo dõi nhịp xuất hàng định kỳ và ưu tiên nhập thêm các mặt hàng có lưu lượng xuất cao.");
        return sb.toString();
    }
}
