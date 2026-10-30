package com.syncstock.supplier.controller;

import com.syncstock.common.dto.PagedResult;
import com.syncstock.supplier.dto.SupplierAnalyticsDto;
import com.syncstock.supplier.dto.SupplierUpsertRequest;
import com.syncstock.supplier.entity.SupplierInfo;
import com.syncstock.supplier.service.SupplierService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/supplier")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    public ResponseEntity<PagedResult<SupplierInfo>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "50") int pageSize
    ) {
        PagedResult<SupplierInfo> list = supplierService.getAll(search, pageNumber, pageSize);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierInfo> getById(@PathVariable Integer id) {
        SupplierInfo supplier = supplierService.getById(id);
        return ResponseEntity.ok(supplier);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('Admin', 'ThuKho')")
    public ResponseEntity<SupplierInfo> create(@Valid @RequestBody SupplierUpsertRequest request) {
        SupplierInfo created = supplierService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Admin', 'ThuKho')")
    public ResponseEntity<SupplierInfo> update(
            @PathVariable Integer id,
            @Valid @RequestBody SupplierUpsertRequest request
    ) {
        SupplierInfo updated = supplierService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Admin', 'ThuKho')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        supplierService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyAuthority('Admin', 'ThuKho', 'KeToan')")
    public ResponseEntity<List<SupplierAnalyticsDto>> getAnalytics(
            @RequestParam(defaultValue = "1000") int limit,
            HttpServletRequest request
    ) {
        String bearerToken = resolveBearerToken(request);
        List<SupplierAnalyticsDto> result = supplierService.getAnalytics(limit, bearerToken);
        return ResponseEntity.ok(result);
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && !authHeader.isBlank()) {
            return authHeader.trim();
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                    return "Bearer " + cookie.getValue().trim();
                }
            }
        }

        return null;
    }
}
