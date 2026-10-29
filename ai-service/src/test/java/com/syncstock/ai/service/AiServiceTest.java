package com.syncstock.ai.service;

import com.syncstock.ai.client.InventoryClient;
import com.syncstock.ai.dto.ChatResponse;
import com.syncstock.ai.dto.StockItemDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests cho AiService.buildInventoryContext() - đảm bảo logic tối ưu
 * context window hoạt động đúng với các trường hợp biên.
 */
@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock
    private InventoryClient inventoryClient;

    private AiService aiService;

    @BeforeEach
    void setUp() {
        // Khởi tạo AiService với apiKey rỗng (không gọi Groq API trong unit test)
        aiService = new AiService(inventoryClient, "", "llama-3.1-8b-instant", "https://api.groq.com/openai/v1");
    }

    // =========================================================
    // Helper methods
    // =========================================================

    private StockItemDto buildItem(int id, String name, String category, int stock, int minStock) {
        return StockItemDto.builder()
                .id(id)
                .name(name)
                .category(category)
                .unit("Cái")
                .stock(stock)
                .minStock(minStock)
                .isLowStock(stock < minStock)
                .build();
    }

    /** Tạo list sản phẩm với tên "Sản phẩm X" và category "Danh mục Y" */
    private List<StockItemDto> buildLargeStockList(int count) {
        List<StockItemDto> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(buildItem(i, "Sản phẩm " + i, "Danh mục " + (i % 5 + 1), 100, 10));
        }
        return list;
    }

    // =========================================================
    // Test cases — buildInventoryContext
    // =========================================================

    @Test
    @DisplayName("buildInventoryContext: Danh sách null -> trả thông báo kho trống")
    void buildInventoryContext_NullList_ReturnsEmptyMessage() {
        String result = aiService.buildInventoryContext(null, "");
        assertThat(result).contains("chưa có sản phẩm");
    }

    @Test
    @DisplayName("buildInventoryContext: Danh sách rỗng -> trả thông báo kho trống")
    void buildInventoryContext_EmptyList_ReturnsEmptyMessage() {
        String result = aiService.buildInventoryContext(Collections.emptyList(), "bàn phím");
        assertThat(result).contains("chưa có sản phẩm");
    }

    @Test
    @DisplayName("buildInventoryContext: < 30 sản phẩm -> hiển thị toàn bộ, không truncate")
    void buildInventoryContext_SmallList_ShowsAll() {
        List<StockItemDto> stockList = buildLargeStockList(15);
        String result = aiService.buildInventoryContext(stockList, "");

        // Phải hiển thị tất cả 15 sản phẩm
        for (int i = 1; i <= 15; i++) {
            assertThat(result).contains("Sản phẩm " + i);
        }
        // Không có thông báo truncation
        assertThat(result).doesNotContain("... (còn");
        assertThat(result).doesNotContain("liên quan)");
    }

    @Test
    @DisplayName("buildInventoryContext: Đúng 30 sản phẩm -> hiển thị toàn bộ, không truncate")
    void buildInventoryContext_ExactlyThirtyItems_ShowsAllWithoutTruncation() {
        List<StockItemDto> stockList = buildLargeStockList(30);
        String result = aiService.buildInventoryContext(stockList, "");

        assertThat(result).contains("Sản phẩm 30");
        assertThat(result).doesNotContain("... (còn");
    }

    @Test
    @DisplayName("buildInventoryContext: > 30 sản phẩm, câu hỏi rỗng -> chỉ hiển thị 30 sản phẩm, có thông báo truncation")
    void buildInventoryContext_LargeList_EmptyQuestion_LimitsTo30() {
        List<StockItemDto> stockList = buildLargeStockList(50);
        String result = aiService.buildInventoryContext(stockList, "");

        // Phải có thông báo truncation với tổng 50
        assertThat(result).contains("50");
        assertThat(result).contains("... (còn");
    }

    @Test
    @DisplayName("buildInventoryContext: > 30 sản phẩm, có từ khóa -> ưu tiên sản phẩm khớp từ khóa xuất hiện trước")
    void buildInventoryContext_LargeList_KeywordMatch_PrioritizesMatchedItems() {
        List<StockItemDto> stockList = buildLargeStockList(40);
        // Thêm sản phẩm đặc biệt ở cuối danh sách (sẽ bị cắt nếu không có keyword priority)
        stockList.add(buildItem(99, "Bàn phím cơ Cherry MX", "Thiết bị ngoại vi", 5, 10));

        // Câu hỏi có từ khóa "bàn phím"
        String result = aiService.buildInventoryContext(stockList, "Còn bao nhiêu bàn phím?");

        // Sản phẩm "Bàn phím cơ Cherry MX" phải xuất hiện trong context (dù ở cuối list)
        assertThat(result).contains("Bàn phím cơ Cherry MX");
    }

    @Test
    @DisplayName("buildInventoryContext: > 30 sản phẩm, keyword match theo category -> sản phẩm trong danh mục khớp được ưu tiên")
    void buildInventoryContext_LargeList_CategoryKeywordMatch_PrioritizesCategory() {
        List<StockItemDto> stockList = buildLargeStockList(35);
        // Thêm sản phẩm đặc biệt với category đặc biệt ở cuối
        stockList.add(buildItem(100, "Ghế công thái học Ergotron", "Nội thất văn phòng", 8, 5));

        String result = aiService.buildInventoryContext(stockList, "Tồn kho nội thất văn phòng còn bao nhiêu?");

        // Sản phẩm trong category "Nội thất văn phòng" phải xuất hiện
        assertThat(result).contains("Ghế công thái học Ergotron");
    }

    @Test
    @DisplayName("buildInventoryContext: LUÔN bao gồm đầy đủ mặt hàng thiếu dù list > 30")
    void buildInventoryContext_LargeList_AlwaysIncludesLowStockSection() {
        List<StockItemDto> stockList = buildLargeStockList(50); // Tất cả đủ hàng (50 items)
        // Thêm 3 sản phẩm thiếu hàng ở cuối (positions 51-53, ngoài top 30 detail items)
        stockList.add(buildItem(51, "Mực in HP 802", "Văn phòng phẩm", 1, 20));
        stockList.add(buildItem(52, "Giấy in A4", "Văn phòng phẩm", 2, 50));
        stockList.add(buildItem(53, "Toner Canon", "Văn phòng phẩm", 0, 10));

        String result = aiService.buildInventoryContext(stockList, "tổng kho");

        // 1. Thống kê tổng quan phải chính xác
        assertThat(result).contains("Số mặt hàng sắp hết/cảnh báo thiếu: 3");

        // 2. Phần "Danh sách thiếu hàng" (luôn đầy đủ, dùng prefix "+") phải có cả 3 mặt hàng
        //    Dù chúng nằm ngoài top-30 trong phần chi tiết
        assertThat(result).contains("+ Mực in HP 802");
        assertThat(result).contains("+ Giấy in A4");
        assertThat(result).contains("+ Toner Canon");

        // 3. Phần chi tiết bị truncate (53 items > 30)
        assertThat(result).contains("... (còn");
    }

    @Test
    @DisplayName("buildInventoryContext: Mặt hàng thiếu nằm trong top 30 -> hiển thị [CẢNH BÁO] trong phần chi tiết")
    void buildInventoryContext_LowStockInTop30_ShowsWarningTagInDetail() {
        // Tạo list > 30 sản phẩm trong đó sản phẩm thiếu hàng nằm ở đầu (sẽ vào top 30)
        List<StockItemDto> stockList = new ArrayList<>();
        // Thêm sản phẩm thiếu hàng ở đầu (positions 1-3, trong top 30)
        stockList.add(buildItem(1, "Mực in HP 802", "Văn phòng phẩm", 1, 20));
        stockList.add(buildItem(2, "Giấy in A4", "Văn phòng phẩm", 2, 50));
        // Thêm 35 sản phẩm đủ hàng
        for (int i = 3; i <= 37; i++) {
            stockList.add(buildItem(i, "Sản phẩm " + i, "Danh mục X", 100, 10));
        }

        String result = aiService.buildInventoryContext(stockList, "");

        // Sản phẩm thiếu hàng nằm trong top 30 → phải hiển thị [CẢNH BÁO: THIẾU HÀNG] trong phần chi tiết
        assertThat(result).contains("[CẢNH BÁO: THIẾU HÀNG]");
        assertThat(result).contains("Mực in HP 802");
        assertThat(result).contains("Giấy in A4");
    }

    @Test
    @DisplayName("buildInventoryContext: Thống kê tổng quan luôn chính xác")
    void buildInventoryContext_SummaryStatsAreAlwaysCorrect() {
        List<StockItemDto> stockList = List.of(
                buildItem(1, "Laptop", "Điện tử", 50, 10),
                buildItem(2, "Chuột", "Điện tử", 3, 20),  // low stock: 3 < 20
                buildItem(3, "Bàn phím", "Điện tử", 100, 5)
        );

        String result = aiService.buildInventoryContext(stockList, "");

        assertThat(result).contains("Tổng số loại mặt hàng: 3");
        assertThat(result).contains("Tổng số lượng hàng hóa trong kho: 153"); // 50+3+100
        assertThat(result).contains("Số mặt hàng sắp hết/cảnh báo thiếu: 1");
        assertThat(result).contains("Chuột"); // low-stock item listed in warning section
    }

    @Test
    @DisplayName("buildInventoryContext: Câu hỏi null -> không gây NPE, xử lý như câu hỏi rỗng")
    void buildInventoryContext_NullQuestion_HandledGracefully() {
        List<StockItemDto> stockList = buildLargeStockList(40);
        // Không được ném NPE khi userQuestion là null
        String result = aiService.buildInventoryContext(stockList, null);
        assertThat(result).isNotBlank();
        assertThat(result).contains("Tổng số loại mặt hàng: 40");
    }

    @Test
    @DisplayName("buildInventoryContext: Sản phẩm có name/category null -> không gây NPE")
    void buildInventoryContext_ItemWithNullFields_HandledGracefully() {
        List<StockItemDto> stockList = List.of(
                buildItem(1, null, null, 50, 10),
                buildItem(2, "Chuột", null, 3, 20)
        );
        // Không được ném NPE
        String result = aiService.buildInventoryContext(stockList, "chuột");
        assertThat(result).isNotBlank();
        assertThat(result).contains("Chuột");
    }

    // =========================================================
    // Test cases — apiKey validation (P2-L-09)
    // =========================================================

    @Test
    @DisplayName("AiService constructor: apiKey rỗng -> apiKey nội bộ = rỗng, không gọi Groq")
    void constructor_EmptyApiKey_DisablesGroq() throws Exception {
        AiService service = new AiService(inventoryClient, "", "llama-3.1-8b-instant", "https://api.groq.com/openai/v1");
        Field field = AiService.class.getDeclaredField("apiKey");
        field.setAccessible(true);
        String key = (String) field.get(service);
        assertThat(key).isEmpty();
    }

    @Test
    @DisplayName("AiService constructor: apiKey quá ngắn (< 10 ký tự) -> apiKey nội bộ = rỗng, không gọi Groq")
    void constructor_TooShortApiKey_DisablesGroq() throws Exception {
        AiService service = new AiService(inventoryClient, "short123", "llama-3.1-8b-instant", "https://api.groq.com/openai/v1");
        Field field = AiService.class.getDeclaredField("apiKey");
        field.setAccessible(true);
        String key = (String) field.get(service);
        assertThat(key).isEmpty();
    }

    @Test
    @DisplayName("AiService constructor: apiKey hợp lệ (>= 10 ký tự) -> apiKey nội bộ giữ nguyên")
    void constructor_ValidApiKey_KeepsKey() throws Exception {
        String validKey = "gsk_valid1234567890abcdef";
        AiService service = new AiService(inventoryClient, validKey, "llama-3.1-8b-instant", "https://api.groq.com/openai/v1");
        Field field = AiService.class.getDeclaredField("apiKey");
        field.setAccessible(true);
        String key = (String) field.get(service);
        assertThat(key).isEqualTo(validKey);
    }

    @Test
    @DisplayName("chat: apiKey rỗng -> fallback rule-based, không ném exception")
    void chat_EmptyApiKey_FallsBackToRuleBased() {
        List<StockItemDto> stock = buildLargeStockList(5);
        when(inventoryClient.getStock()).thenReturn(stock);

        AiService service = new AiService(inventoryClient, "", "llama-3.1-8b-instant", "https://api.groq.com/openai/v1");
        ChatResponse response = service.chat(com.syncstock.ai.dto.ChatRequest.builder().message("tổng kho").build());

        assertThat(response).isNotNull();
        assertThat(response.getAnswer()).isNotBlank();
    }
}

