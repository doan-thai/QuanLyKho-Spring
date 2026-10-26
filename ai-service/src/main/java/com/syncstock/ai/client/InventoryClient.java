package com.syncstock.ai.client;

import com.syncstock.ai.dto.StockItemDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class InventoryClient {

    private final RestClient restClient;

    public InventoryClient(
            RestClient.Builder restClientBuilder,
            @Value("${inventory.service.url:http://localhost:5002}") String inventoryServiceUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(inventoryServiceUrl)
                .build();
    }

    public List<StockItemDto> getStock() {
        try {
            List<StockItemDto> stock = restClient.get()
                    .uri("/api/inventory/stock")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            return stock != null ? stock : Collections.emptyList();
        } catch (Exception ex) {
            log.error("Failed to fetch stock from inventory-service: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }
}
