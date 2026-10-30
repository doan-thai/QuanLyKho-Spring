package com.syncstock.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncstock.ai.dto.StockItemDto;
import com.syncstock.common.dto.PagedResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class InventoryClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("H-10: InventoryClient.getStock lấy dữ liệu từ PagedResult của inventory-service")
    void getStock_ShouldReturnDataFromPagedResult() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        InventoryClient client = new InventoryClient(builder, "http://localhost:5002");

        PagedResult<StockItemDto> pagedResult = PagedResult.<StockItemDto>builder()
                .totalRecords(1)
                .pageNumber(1)
                .pageSize(100)
                .totalPages(1)
                .data(List.of(
                        StockItemDto.builder()
                                .id(1)
                                .name("Laptop")
                                .stock(50)
                                .minStock(10)
                                .isLowStock(false)
                                .build()
                ))
                .build();

        server.expect(requestTo("http://localhost:5002/api/inventory/stock"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(pagedResult), MediaType.APPLICATION_JSON));

        List<StockItemDto> result = client.getStock();

        server.verify();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Laptop", result.get(0).getName());
    }
}

