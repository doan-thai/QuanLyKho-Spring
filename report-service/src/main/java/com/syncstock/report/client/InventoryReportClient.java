package com.syncstock.report.client;

import com.syncstock.common.dto.PagedResult;
import com.syncstock.report.dto.StockReportItemDto;
import com.syncstock.report.dto.TransactionReportItemDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class InventoryReportClient {

    private final RestClient restClient;

    public InventoryReportClient(
            RestClient.Builder restClientBuilder,
            @Value("${inventory.service.url:http://localhost:5002}") String inventoryServiceUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(inventoryServiceUrl)
                .build();
    }

    public List<StockReportItemDto> getStock() {
        return getStock(null);
    }

    public List<StockReportItemDto> getStock(String bearerToken) {
        try {
            var requestSpec = restClient.get()
                    .uri("/api/inventory/stock");

            if (bearerToken != null && !bearerToken.isBlank()) {
                String token = bearerToken.trim();
                if (!token.regionMatches(true, 0, "Bearer ", 0, 7)) {
                    token = "Bearer " + token;
                }
                requestSpec.header(HttpHeaders.AUTHORIZATION, token);
            }

            PagedResult<StockReportItemDto> response = requestSpec
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            return response != null && response.getData() != null ? response.getData() : Collections.emptyList();
        } catch (RestClientResponseException ex) {
            log.error("Error response from inventory-service /api/inventory/stock (status {}): {}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.valueOf(ex.getStatusCode().value()),
                    "Lỗi khi gọi inventory-service: " + ex.getStatusText());
        } catch (Exception ex) {
            log.error("Failed to connect to inventory-service: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Không thể kết nối đến inventory-service.");
        }
    }

    public List<TransactionReportItemDto> getHistory(int limit) {
        return getHistory(limit, null);
    }

    public List<TransactionReportItemDto> getHistory(int limit, String bearerToken) {
        int safeLimit = Math.clamp(limit, 1, 1000);

        try {
            var requestSpec = restClient.get()
                    .uri("/api/inventory/history?limit={limit}", safeLimit);

            if (bearerToken != null && !bearerToken.isBlank()) {
                String token = bearerToken.trim();
                if (!token.regionMatches(true, 0, "Bearer ", 0, 7)) {
                    token = "Bearer " + token;
                }
                requestSpec.header(HttpHeaders.AUTHORIZATION, token);
            }

            PagedResult<TransactionReportItemDto> response = requestSpec
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            return response != null && response.getData() != null ? response.getData() : Collections.emptyList();
        } catch (RestClientResponseException ex) {
            log.error("Error response from inventory-service /api/inventory/history (status {}): {}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.valueOf(ex.getStatusCode().value()),
                    "Lỗi khi gọi inventory-service: " + ex.getStatusText());
        } catch (Exception ex) {
            log.error("Failed to connect to inventory-service: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Không thể kết nối đến inventory-service.");
        }
    }
}
