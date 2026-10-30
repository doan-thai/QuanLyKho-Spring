package com.syncstock.inventory.service;

import com.syncstock.inventory.dto.ExportRequest;
import com.syncstock.inventory.dto.ImportRequest;
import com.syncstock.inventory.dto.ReceiptResponse;
import com.syncstock.inventory.dto.StockItemRequest;
import com.syncstock.inventory.dto.StockItemResponse;
import com.syncstock.inventory.dto.StockTransactionResponse;
import com.syncstock.inventory.entity.ExportReceipt;
import com.syncstock.inventory.entity.ImportReceipt;
import com.syncstock.inventory.entity.Product;
import com.syncstock.inventory.entity.StockTransaction;
import com.syncstock.inventory.repository.ExportReceiptRepository;
import com.syncstock.inventory.repository.ImportReceiptRepository;
import com.syncstock.inventory.repository.ProductRepository;
import com.syncstock.inventory.repository.StockTransactionRepository;
import com.syncstock.inventory.config.RabbitMQConfig;
import com.syncstock.inventory.event.ImportCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.syncstock.inventory.dto.PagedResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;
    private final ImportReceiptRepository importReceiptRepository;
    private final ExportReceiptRepository exportReceiptRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public PagedResult<StockItemResponse> getStockOverview(int pageNumber, int pageSize) {
        int safePage = Math.max(1, pageNumber);
        int safeSize = Math.clamp(pageSize, 1, 500);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize);
        Page<Product> page = productRepository.findAllByOrderByIdAsc(pageable);

        List<StockItemResponse> items = page.getContent().stream()
                .map(p -> StockItemResponse.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .category(p.getCategory())
                        .unit(p.getUnit())
                        .stock(p.getStock())
                        .minStock(p.getMinStock())
                        .imageUrl(p.getImageUrl())
                        .isLowStock(p.getStock() < p.getMinStock())
                        .build())
                .collect(Collectors.toList());

        return PagedResult.<StockItemResponse>builder()
                .totalRecords(page.getTotalElements())
                .pageNumber(safePage)
                .pageSize(safeSize)
                .totalPages(page.getTotalPages())
                .data(items)
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResult<StockItemResponse> getStockOverview() {
        return getStockOverview(1, 100);
    }

    @Transactional(readOnly = true)
    public PagedResult<StockTransactionResponse> getTransactionHistory(int pageNumber, int pageSize) {
        int safePage = Math.max(1, pageNumber);
        int safeLimit = Math.clamp(pageSize, 1, 1000);
        Pageable pageable = PageRequest.of(safePage - 1, safeLimit);
        Page<StockTransactionResponse> page = stockTransactionRepository.findTransactionHistoryPage(pageable);

        return PagedResult.<StockTransactionResponse>builder()
                .totalRecords(page.getTotalElements())
                .pageNumber(safePage)
                .pageSize(safeLimit)
                .totalPages(page.getTotalPages())
                .data(page.getContent())
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResult<StockTransactionResponse> getTransactionHistory(int limit) {
        return getTransactionHistory(1, limit);
    }

    @Transactional(rollbackFor = Exception.class)
    public ReceiptResponse importStock(ImportRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh sách sản phẩm nhập kho không được để trống");
        }

        String ticketCode = request.getCode();
        if (ticketCode == null || ticketCode.isBlank()) {
            ticketCode = generateReceiptCode("PN");
        }

        ImportReceipt receipt = ImportReceipt.builder()
                .code(ticketCode.trim())
                .note(request.getNote() != null ? request.getNote().trim() : "")
                .supplierId(request.getSupplierId())
                .importDate(LocalDateTime.now())
                .build();

        receipt = importReceiptRepository.save(receipt);

        for (StockItemRequest item : request.getItems()) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng nhập kho phải lớn hơn 0");
            }

            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm với ID: " + item.getProductId()));

            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);

            BigDecimal unitPrice = item.getPrice() != null && item.getPrice().compareTo(BigDecimal.ZERO) >= 0
                    ? item.getPrice()
                    : BigDecimal.ZERO;

            String note = (request.getNote() != null && !request.getNote().isBlank())
                    ? "Nhập theo phiếu " + receipt.getId() + " - " + request.getNote().trim()
                    : "Nhập theo phiếu " + receipt.getId();

            StockTransaction transaction = StockTransaction.builder()
                    .productId(item.getProductId())
                    .transactionType("IMPORT")
                    .quantity(item.getQuantity())
                    .unitPrice(unitPrice)
                    .supplierId(request.getSupplierId())
                    .transactionDate(LocalDateTime.now())
                    .note(note)
                    .build();

            stockTransactionRepository.save(transaction);
        }

        log.info("Successfully processed import receipt #{} (code: {}) with {} items.", receipt.getId(), receipt.getCode(), request.getItems().size());

        // Publish sự kiện ImportCompletedEvent (sẽ được gửi tới RabbitMQ SAU KHI commit transaction thành công)
        if (request.getSupplierId() != null) {
            BigDecimal totalAmount = request.getItems().stream()
                    .map(item -> {
                        BigDecimal price = item.getPrice() != null && item.getPrice().compareTo(BigDecimal.ZERO) > 0
                                ? item.getPrice() : BigDecimal.ZERO;
                        int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                        return price.multiply(BigDecimal.valueOf(qty));
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            ImportCompletedEvent event = ImportCompletedEvent.builder()
                    .receiptId(receipt.getId())
                    .supplierId(request.getSupplierId())
                    .totalImportAmount(totalAmount)
                    .importDate(receipt.getImportDate())
                    .build();

            eventPublisher.publishEvent(event);
        }

        return ReceiptResponse.builder()
                .message("Nhập kho thành công!")
                .receiptId(receipt.getId())
                .code(receipt.getCode())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public ReceiptResponse exportStock(ExportRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh sách sản phẩm xuất kho không được để trống");
        }

        String exportReason = (request.getReason() != null && !request.getReason().isBlank())
                ? request.getReason().trim()
                : (request.getNote() != null ? request.getNote().trim() : "");

        String ticketCode = request.getCode();
        if (ticketCode == null || ticketCode.isBlank()) {
            ticketCode = generateReceiptCode("PX");
        }

        ExportReceipt receipt = ExportReceipt.builder()
                .code(ticketCode.trim())
                .reason(!exportReason.isBlank() ? exportReason : "Xuất kho")
                .exportDate(LocalDateTime.now())
                .build();

        receipt = exportReceiptRepository.save(receipt);

        for (StockItemRequest item : request.getItems()) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng xuất kho phải lớn hơn 0");
            }

            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm với ID: " + item.getProductId()));

            // Check stock and throw 400 Bad Request to rollback if insufficient stock
            if (product.getStock() < item.getQuantity()) {
                String errorMsg = String.format("Không đủ hàng: '%s'. Tồn: %d, Yêu cầu: %d.",
                        product.getName(), product.getStock(), item.getQuantity());
                log.warn("Export rejected: {}", errorMsg);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMsg);
            }

            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);

            BigDecimal unitPrice = item.getPrice() != null && item.getPrice().compareTo(BigDecimal.ZERO) >= 0
                    ? item.getPrice()
                    : BigDecimal.ZERO;

            String note = !exportReason.isBlank()
                    ? "Xuất theo phiếu " + receipt.getId() + " - " + exportReason
                    : "Xuất theo phiếu " + receipt.getId();

            StockTransaction transaction = StockTransaction.builder()
                    .productId(item.getProductId())
                    .transactionType("EXPORT")
                    .quantity(item.getQuantity())
                    .unitPrice(unitPrice)
                    .supplierId(null)
                    .transactionDate(LocalDateTime.now())
                    .note(note)
                    .build();

            stockTransactionRepository.save(transaction);
        }

        log.info("Successfully processed export receipt #{} (code: {}) with {} items.", receipt.getId(), receipt.getCode(), request.getItems().size());

        return ReceiptResponse.builder()
                .message("Xuất kho thành công!")
                .receiptId(receipt.getId())
                .code(receipt.getCode())
                .build();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleImportCompletedEvent(ImportCompletedEvent event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.IMPORT_ROUTING_KEY, event);
            log.info("[RabbitMQ] Published ImportCompletedEvent after commit for receipt #{} -> supplierId={}, amount={}",
                    event.getReceiptId(), event.getSupplierId(), event.getTotalImportAmount());
        } catch (Exception ex) {
            log.warn("[RabbitMQ] Failed to publish ImportCompletedEvent after commit: {}", ex.getMessage());
        }
    }

    private String generateReceiptCode(String prefix) {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        int randomPart = ThreadLocalRandom.current().nextInt(100, 1000);
        return prefix + "-" + timestamp + "-" + randomPart;
    }
}
