package com.syncstock.report.service;

import com.syncstock.report.client.InventoryReportClient;
import com.syncstock.report.dto.BusinessReportResponse;
import com.syncstock.report.dto.StockReportItemDto;
import com.syncstock.report.dto.StockReportResponse;
import com.syncstock.report.dto.TopSellingProductDto;
import com.syncstock.report.dto.TransactionReportItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final InventoryReportClient inventoryReportClient;

    public StockReportResponse getStockReport() {
        List<StockReportItemDto> stockList = inventoryReportClient.getStock();

        int totalProducts = stockList.size();
        long totalStock = stockList.stream()
                .mapToLong(item -> item.getStock() != null ? item.getStock() : 0)
                .sum();
        int lowStockCount = (int) stockList.stream()
                .filter(StockReportItemDto::isLowStock)
                .count();

        return StockReportResponse.builder()
                .totalProducts(totalProducts)
                .totalStock(totalStock)
                .lowStockCount(lowStockCount)
                .items(stockList)
                .build();
    }

    public List<TransactionReportItemDto> getTransactions(String from, String to, String type, int limit) {
        int safeLimit = Math.clamp(limit, 1, 5000);
        List<TransactionReportItemDto> rawHistory = inventoryReportClient.getHistory(safeLimit);

        LocalDateTime fromDate = parseFromDate(from);
        LocalDateTime toDate = parseToDate(to);

        return rawHistory.stream()
                .filter(tx -> isWithinDateRange(tx.getDate(), fromDate, toDate))
                .filter(tx -> matchesTransactionType(tx.getType(), type))
                .toList();
    }

    public BusinessReportResponse getBusinessReport(String from, String to, int top) {
        List<TransactionReportItemDto> allTransactions = inventoryReportClient.getHistory(5000);

        LocalDateTime fromDate = parseFromDate(from);
        LocalDateTime toDate = parseToDate(to);

        List<TransactionReportItemDto> filtered = allTransactions.stream()
                .filter(tx -> isWithinDateRange(tx.getDate(), fromDate, toDate))
                .toList();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        int totalImportCount = 0;
        int totalExportCount = 0;
        long totalQuantityImported = 0;
        long totalQuantityExported = 0;

        Map<Integer, ProductSaleAggregator> saleAggregators = new HashMap<>();

        for (TransactionReportItemDto tx : filtered) {
            String txType = tx.getType() != null ? tx.getType().toUpperCase() : "";
            int quantity = tx.getQuantity() != null ? tx.getQuantity() : 0;
            BigDecimal amount = tx.getTotalAmount();
            if (amount == null) {
                BigDecimal unitPrice = tx.getUnitPrice() != null ? tx.getUnitPrice() : BigDecimal.ZERO;
                amount = unitPrice.multiply(BigDecimal.valueOf(quantity));
            }

            if (isImportType(txType)) {
                totalCost = totalCost.add(amount);
                totalImportCount++;
                totalQuantityImported += quantity;
            } else if (isExportType(txType)) {
                totalRevenue = totalRevenue.add(amount);
                totalExportCount++;
                totalQuantityExported += quantity;

                Integer productId = tx.getProductId();
                if (productId != null) {
                    ProductSaleAggregator agg = saleAggregators.computeIfAbsent(
                            productId,
                            id -> new ProductSaleAggregator(id, tx.getProductName())
                    );
                    agg.addSale(quantity, amount);
                }
            }
        }

        BigDecimal grossProfit = totalRevenue.subtract(totalCost);

        int maxTop = top > 0 ? top : 10;
        List<TopSellingProductDto> topSellingProducts = saleAggregators.values().stream()
                .map(ProductSaleAggregator::toDto)
                .sorted(Comparator.comparing(TopSellingProductDto::getQuantitySold).reversed()
                        .thenComparing(TopSellingProductDto::getTotalRevenue, Comparator.reverseOrder()))
                .limit(maxTop)
                .toList();

        return BusinessReportResponse.builder()
                .from(from)
                .to(to)
                .totalRevenue(totalRevenue)
                .totalCost(totalCost)
                .grossProfit(grossProfit)
                .totalImportCount(totalImportCount)
                .totalExportCount(totalExportCount)
                .totalQuantityImported(totalQuantityImported)
                .totalQuantityExported(totalQuantityExported)
                .topSellingProducts(topSellingProducts)
                .build();
    }

    public byte[] exportReport(String from, String to, String reportType, String format) throws IOException {
        String safeType = (reportType != null ? reportType.trim().toLowerCase() : "inventory");
        String safeFormat = (format != null ? format.trim().toLowerCase() : "xlsx");

        if ("csv".equals(safeFormat)) {
            return exportCsv(from, to, safeType);
        } else {
            return exportExcel(from, to, safeType);
        }
    }

    private byte[] exportExcel(String from, String to, String reportType) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            if ("inventory".equals(reportType)) {
                Sheet sheet = workbook.createSheet("Bao_cao_ton_kho");
                String[] headers = {"Mã SP", "Tên sản phẩm", "Danh mục", "Đơn vị tính", "Tồn kho", "Mức tối thiểu", "Cảnh báo hết hàng"};
                createHeaderRow(sheet, headers, headerStyle);

                List<StockReportItemDto> stockList = inventoryReportClient.getStock();
                int rowIdx = 1;
                for (StockReportItemDto item : stockList) {
                    Row row = sheet.createRow(rowIdx++);
                    createCell(row, 0, item.getId(), dataStyle);
                    createCell(row, 1, item.getName(), dataStyle);
                    createCell(row, 2, item.getCategory(), dataStyle);
                    createCell(row, 3, item.getUnit(), dataStyle);
                    createCell(row, 4, item.getStock(), dataStyle);
                    createCell(row, 5, item.getMinStock(), dataStyle);
                    createCell(row, 6, item.isLowStock() ? "Thiếu hàng" : "Đủ hàng", dataStyle);
                }
                autoSizeColumns(sheet, headers.length);
            } else {
                boolean isInward = "inward".equals(reportType);
                Sheet sheet = workbook.createSheet(isInward ? "Bao_cao_nhap_kho" : "Bao_cao_xuat_kho");
                String[] headers = {"Mã GD", "Mã SP", "Tên sản phẩm", "Loại", "Số lượng", "Đơn giá", "Thành tiền", "Thời gian", "Ghi chú"};
                createHeaderRow(sheet, headers, headerStyle);

                List<TransactionReportItemDto> transactions = getTransactions(from, to, isInward ? "IMPORT" : "EXPORT", 5000);
                int rowIdx = 1;
                for (TransactionReportItemDto tx : transactions) {
                    Row row = sheet.createRow(rowIdx++);
                    createCell(row, 0, tx.getId(), dataStyle);
                    createCell(row, 1, tx.getProductId(), dataStyle);
                    createCell(row, 2, tx.getProductName(), dataStyle);
                    createCell(row, 3, tx.getType(), dataStyle);
                    createCell(row, 4, tx.getQuantity(), dataStyle);
                    createCell(row, 5, tx.getUnitPrice() != null ? tx.getUnitPrice().toPlainString() : "0", dataStyle);
                    createCell(row, 6, tx.getTotalAmount() != null ? tx.getTotalAmount().toPlainString() : "0", dataStyle);
                    createCell(row, 7, tx.getDate() != null ? tx.getDate().toString() : "", dataStyle);
                    createCell(row, 8, tx.getNote(), dataStyle);
                }
                autoSizeColumns(sheet, headers.length);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private byte[] exportCsv(String from, String to, String reportType) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Ghi UTF-8 BOM để Excel hiển thị đúng tiếng Việt
        out.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

        StringBuilder sb = new StringBuilder();

        if ("inventory".equals(reportType)) {
            sb.append("ProductId,ProductName,Category,Unit,Stock,MinStock,IsLowStock\n");
            List<StockReportItemDto> stockList = inventoryReportClient.getStock();
            for (StockReportItemDto item : stockList) {
                sb.append(csvEscape(item.getId())).append(",")
                        .append(csvEscape(item.getName())).append(",")
                        .append(csvEscape(item.getCategory())).append(",")
                        .append(csvEscape(item.getUnit())).append(",")
                        .append(csvEscape(item.getStock())).append(",")
                        .append(csvEscape(item.getMinStock())).append(",")
                        .append(item.isLowStock() ? "Yes" : "No").append("\n");
            }
        } else {
            boolean isInward = "inward".equals(reportType);
            sb.append("TransactionId,ProductId,ProductName,Type,Quantity,UnitPrice,TotalAmount,Date,Note\n");
            List<TransactionReportItemDto> transactions = getTransactions(from, to, isInward ? "IMPORT" : "EXPORT", 5000);
            for (TransactionReportItemDto tx : transactions) {
                sb.append(csvEscape(tx.getId())).append(",")
                        .append(csvEscape(tx.getProductId())).append(",")
                        .append(csvEscape(tx.getProductName())).append(",")
                        .append(csvEscape(tx.getType())).append(",")
                        .append(csvEscape(tx.getQuantity())).append(",")
                        .append(csvEscape(tx.getUnitPrice())).append(",")
                        .append(csvEscape(tx.getTotalAmount())).append(",")
                        .append(csvEscape(tx.getDate())).append(",")
                        .append(csvEscape(tx.getNote())).append("\n");
            }
        }

        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        setBorders(style);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        setBorders(style);
        return style;
    }

    private void setBorders(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private void createHeaderRow(Sheet sheet, String[] headers, CellStyle headerStyle) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void createCell(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Number num) {
            cell.setCellValue(num.doubleValue());
        } else if (value instanceof Boolean b) {
            cell.setCellValue(b ? "Yes" : "No");
        } else {
            cell.setCellValue(value.toString());
        }
        cell.setCellStyle(style);
    }

    private void autoSizeColumns(Sheet sheet, int numColumns) {
        for (int i = 0; i < numColumns; i++) {
            sheet.autoSizeColumn(i);
            // Giữ độ rộng tối thiểu để nhìn đẹp
            if (sheet.getColumnWidth(i) < 3000) {
                sheet.setColumnWidth(i, 3500);
            }
        }
    }

    private String csvEscape(Object value) {
        if (value == null) {
            return "";
        }
        String str = value.toString();
        if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }

    private boolean isWithinDateRange(LocalDateTime txDate, LocalDateTime from, LocalDateTime to) {
        if (txDate == null) {
            return true;
        }
        if (from != null && txDate.isBefore(from)) {
            return false;
        }
        if (to != null && txDate.isAfter(to)) {
            return false;
        }
        return true;
    }

    private boolean matchesTransactionType(String txType, String targetType) {
        if (targetType == null || targetType.isBlank() || "ALL".equalsIgnoreCase(targetType.trim())) {
            return true;
        }
        String safeTxType = txType != null ? txType.trim().toUpperCase() : "";
        String normalizedTarget = targetType.trim().toUpperCase();

        if (isImportType(normalizedTarget)) {
            return isImportType(safeTxType);
        }
        if (isExportType(normalizedTarget)) {
            return isExportType(safeTxType);
        }
        return safeTxType.equalsIgnoreCase(normalizedTarget);
    }

    private boolean isImportType(String type) {
        return type.contains("IMPORT") || type.contains("IN") || type.contains("NHAP");
    }

    private boolean isExportType(String type) {
        return type.contains("EXPORT") || type.contains("OUT") || type.contains("XUAT");
    }

    private LocalDateTime parseFromDate(String from) {
        if (from == null || from.isBlank()) {
            return null;
        }
        try {
            String trimmed = from.trim();
            if (trimmed.length() == 10) {
                return LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
            }
            return LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception ex) {
            log.warn("Cannot parse from date '{}': {}", from, ex.getMessage());
            return null;
        }
    }

    private LocalDateTime parseToDate(String to) {
        if (to == null || to.isBlank()) {
            return null;
        }
        try {
            String trimmed = to.trim();
            if (trimmed.length() == 10) {
                return LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE).atTime(23, 59, 59, 999999999);
            }
            return LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception ex) {
            log.warn("Cannot parse to date '{}': {}", to, ex.getMessage());
            return null;
        }
    }

    private static class ProductSaleAggregator {
        private final Integer productId;
        private final String productName;
        private long quantitySold = 0;
        private BigDecimal totalRevenue = BigDecimal.ZERO;

        public ProductSaleAggregator(Integer productId, String productName) {
            this.productId = productId;
            this.productName = productName != null ? productName : ("Sản phẩm #" + productId);
        }

        public void addSale(long qty, BigDecimal revenue) {
            this.quantitySold += qty;
            if (revenue != null) {
                this.totalRevenue = this.totalRevenue.add(revenue);
            }
        }

        public TopSellingProductDto toDto() {
            return TopSellingProductDto.builder()
                    .productId(productId)
                    .productName(productName)
                    .quantitySold(quantitySold)
                    .totalRevenue(totalRevenue)
                    .build();
        }
    }
}
