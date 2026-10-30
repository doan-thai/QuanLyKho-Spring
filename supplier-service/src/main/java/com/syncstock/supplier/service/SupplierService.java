package com.syncstock.supplier.service;

import com.syncstock.common.dto.PagedResult;
import com.syncstock.supplier.client.InventoryAnalyticsClient;
import com.syncstock.supplier.dto.InventoryHistoryDto;
import com.syncstock.supplier.dto.SupplierAnalyticsDto;
import com.syncstock.supplier.dto.SupplierUpsertRequest;
import com.syncstock.supplier.entity.SupplierInfo;
import com.syncstock.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final InventoryAnalyticsClient inventoryAnalyticsClient;

    @Transactional(readOnly = true)
    public PagedResult<SupplierInfo> getAll(String search, int pageNumber, int pageSize) {
        int safePage = Math.max(1, pageNumber);
        int safeSize = Math.clamp(pageSize, 1, 500);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize);

        Page<SupplierInfo> page;
        if (search == null || search.trim().isBlank()) {
            page = supplierRepository.findAllByOrderByNameAsc(pageable);
        } else {
            page = supplierRepository.searchSuppliers(search.trim(), pageable);
        }

        return PagedResult.<SupplierInfo>builder()
                .totalRecords(page.getTotalElements())
                .pageNumber(safePage)
                .pageSize(safeSize)
                .totalPages(page.getTotalPages())
                .data(page.getContent())
                .build();
    }

    @Transactional(readOnly = true)
    public List<SupplierInfo> getAll(String search) {
        return getAll(search, 1, 500).getData();
    }

    @Transactional(readOnly = true)
    public SupplierInfo getById(Integer id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhà cung cấp."));
    }

    @Transactional
    public SupplierInfo create(SupplierUpsertRequest request) {
        String trimmedName = request.getName().trim();

        if (supplierRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên nhà cung cấp đã tồn tại.");
        }

        SupplierInfo supplier = SupplierInfo.builder()
                .name(trimmedName)
                .contactPerson(normalizeField(request.getContactPerson()))
                .phone(request.getPhone().trim())
                .email(normalizeField(request.getEmail()))
                .address(normalizeField(request.getAddress()))
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .createdAt(LocalDateTime.now())
                .totalTransactions(0)
                .totalImportValue(BigDecimal.ZERO)
                .reliabilityScore(Boolean.FALSE.equals(request.getIsActive())
                        ? new BigDecimal("25.00")
                        : new BigDecimal("50.00"))
                .lastTransactionAt(null)
                .build();

        SupplierInfo saved = supplierRepository.save(supplier);
        log.info("Created supplier #{} - {}", saved.getId(), saved.getName());
        return saved;
    }

    @Transactional
    public SupplierInfo update(Integer id, SupplierUpsertRequest request) {
        SupplierInfo supplier = getById(id);
        String trimmedName = request.getName().trim();

        if (supplierRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên nhà cung cấp đã tồn tại ở bản ghi khác.");
        }

        supplier.setName(trimmedName);
        supplier.setContactPerson(normalizeField(request.getContactPerson()));
        supplier.setPhone(request.getPhone().trim());
        supplier.setEmail(normalizeField(request.getEmail()));
        supplier.setAddress(normalizeField(request.getAddress()));

        if (request.getIsActive() != null) {
            supplier.setIsActive(request.getIsActive());
        }

        SupplierInfo updated = supplierRepository.save(supplier);
        log.info("Updated supplier #{} - {}", updated.getId(), updated.getName());
        return updated;
    }

    @Transactional
    public void delete(Integer id) {
        SupplierInfo supplier = getById(id);
        supplierRepository.delete(supplier);
        log.info("Deleted supplier #{} - {}", id, supplier.getName());
    }

    public List<SupplierAnalyticsDto> getAnalytics(int limit, String bearerToken) {
        int safeLimit = Math.clamp(limit, 1, 1000);
        List<InventoryHistoryDto> history = inventoryAnalyticsClient.getHistory(safeLimit, bearerToken);

        List<InventoryHistoryDto> importTransactions = history.stream()
                .filter(item -> isImport(item) && item.getSupplierId() != null)
                .toList();

        Map<Integer, List<InventoryHistoryDto>> grouped = importTransactions.stream()
                .collect(Collectors.groupingBy(InventoryHistoryDto::getSupplierId));

        List<SupplierInfo> suppliers = supplierRepository.findAllByOrderByNameAsc();
        List<SupplierAnalyticsDto> analyticsList = new ArrayList<>(suppliers.size());

        for (SupplierInfo supplier : suppliers) {
            List<InventoryHistoryDto> supplierTxs = grouped.get(supplier.getId());

            int count = 0;
            BigDecimal totalValue = BigDecimal.ZERO;
            LocalDateTime lastDate = null;
            BigDecimal reliabilityScore;

            if (supplierTxs == null || supplierTxs.isEmpty()) {
                reliabilityScore = Boolean.TRUE.equals(supplier.getIsActive())
                        ? new BigDecimal("50.00")
                        : new BigDecimal("25.00");
            } else {
                count = supplierTxs.size();
                totalValue = supplierTxs.stream()
                        .map(this::resolveAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                lastDate = supplierTxs.stream()
                        .map(this::resolveDate)
                        .filter(Objects::nonNull)
                        .max(LocalDateTime::compareTo)
                        .orElse(null);

                reliabilityScore = calculateReliabilityScore(supplier, count, totalValue, lastDate);
            }

            SupplierAnalyticsDto dto = SupplierAnalyticsDto.builder()
                    .id(supplier.getId())
                    .name(supplier.getName())
                    .contactPerson(supplier.getContactPerson())
                    .phone(supplier.getPhone())
                    .email(supplier.getEmail())
                    .address(supplier.getAddress())
                    .isActive(supplier.getIsActive())
                    .createdAt(supplier.getCreatedAt())
                    .totalTransactions(count)
                    .totalImportValue(totalValue)
                    .reliabilityScore(reliabilityScore)
                    .lastTransactionAt(lastDate)
                    .build();

            analyticsList.add(dto);
        }

        log.info("Computed analytics in-memory for {} suppliers based on {} import transactions. (No DB write)",
                analyticsList.size(), importTransactions.size());
        return analyticsList;
    }

    private BigDecimal calculateReliabilityScore(SupplierInfo supplier, int count, BigDecimal totalValue, LocalDateTime lastDate) {
        double ageDays = 0.0;
        if (lastDate != null) {
            long seconds = Duration.between(lastDate, LocalDateTime.now()).getSeconds();
            ageDays = Math.max(0.0, seconds / 86400.0);
        }

        double activityScore = Math.min(50.0, count * 2.0);
        double totalValDouble = totalValue != null ? totalValue.doubleValue() : 0.0;
        double valueScore = Math.min(30.0, (totalValDouble / 100_000_000.0) * 30.0);
        double recencyScore = ageDays <= 30.0 ? 20.0 : (ageDays <= 90.0 ? 12.0 : 6.0);

        double score = activityScore + valueScore + recencyScore;
        if (Boolean.FALSE.equals(supplier.getIsActive())) {
            score *= 0.5;
        }

        double clampedScore = Math.clamp(score, 0.0, 100.0);
        return BigDecimal.valueOf(clampedScore).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isImport(InventoryHistoryDto item) {
        String type = item.getType();
        if (type == null || type.isBlank()) {
            type = item.getTransactionType();
        }
        return type != null && type.toUpperCase().contains("IMPORT");
    }

    private LocalDateTime resolveDate(InventoryHistoryDto item) {
        if (item.getDate() != null) {
            return item.getDate();
        }
        return item.getTransactionDate();
    }

    private BigDecimal resolveAmount(InventoryHistoryDto item) {
        if (item.getTotalAmount() != null && item.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            return item.getTotalAmount();
        }

        if (item.getUnitPrice() != null && item.getQuantity() != null
                && item.getUnitPrice().compareTo(BigDecimal.ZERO) > 0 && item.getQuantity() > 0) {
            return item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        }

        return BigDecimal.ZERO;
    }

    private String normalizeField(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }
}
