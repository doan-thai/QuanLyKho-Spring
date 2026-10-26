package com.syncstock.inventory.controller;

import com.syncstock.inventory.dto.ExportRequest;
import com.syncstock.inventory.dto.ImportRequest;
import com.syncstock.inventory.dto.ReceiptResponse;
import com.syncstock.inventory.dto.StockItemResponse;
import com.syncstock.inventory.dto.StockTransactionResponse;
import com.syncstock.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/stock")
    public ResponseEntity<List<StockItemResponse>> getStock() {
        List<StockItemResponse> stock = inventoryService.getStockOverview();
        return ResponseEntity.ok(stock);
    }

    @GetMapping("/history")
    public ResponseEntity<List<StockTransactionResponse>> getHistory(@RequestParam(defaultValue = "200") int limit) {
        List<StockTransactionResponse> history = inventoryService.getTransactionHistory(limit);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('Admin', 'ThuKho')")
    public ResponseEntity<ReceiptResponse> importStock(@Valid @RequestBody ImportRequest request) {
        ReceiptResponse response = inventoryService.importStock(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/export")
    @PreAuthorize("hasAnyAuthority('Admin', 'ThuKho')")
    public ResponseEntity<ReceiptResponse> exportStock(@Valid @RequestBody ExportRequest request) {
        ReceiptResponse response = inventoryService.exportStock(request);
        return ResponseEntity.ok(response);
    }
}
