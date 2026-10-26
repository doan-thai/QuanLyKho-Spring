package com.syncstock.report.controller;

import com.syncstock.report.dto.BusinessReportResponse;
import com.syncstock.report.dto.StockReportResponse;
import com.syncstock.report.dto.TransactionReportItemDto;
import com.syncstock.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('Admin', 'KeToan')")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/stock")
    public ResponseEntity<StockReportResponse> getStockReport() {
        StockReportResponse report = reportService.getStockReport();
        return ResponseEntity.ok(report);
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionReportItemDto>> getTransactions(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "200") int limit
    ) {
        List<TransactionReportItemDto> list = reportService.getTransactions(from, to, type, limit);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/business")
    public ResponseEntity<BusinessReportResponse> getBusinessReport(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "10") int top
    ) {
        BusinessReportResponse response = reportService.getBusinessReport(from, to, top);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "inventory") String reportType,
            @RequestParam(defaultValue = "xlsx") String format
    ) throws IOException {
        byte[] fileBytes = reportService.exportReport(from, to, reportType, format);

        boolean isCsv = "csv".equalsIgnoreCase(format != null ? format.trim() : "");
        String ext = isCsv ? "csv" : "xlsx";
        String filename = "Bao_cao_" + reportType.trim().toLowerCase() + "_" + System.currentTimeMillis() + "." + ext;

        MediaType mediaType = isCsv
                ? MediaType.parseMediaType("text/csv; charset=UTF-8")
                : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(mediaType)
                .body(fileBytes);
    }
}
