package com.syncstock.inventory.controller;

import com.syncstock.inventory.dto.PagedResult;
import com.syncstock.inventory.dto.ProductUpsertRequest;
import com.syncstock.inventory.dto.UploadImageResponse;
import com.syncstock.inventory.entity.Product;
import com.syncstock.inventory.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

@RestController
@RequestMapping("/api/inventory/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<PagedResult<Product>> getProducts(
            @RequestParam(required = false) String searchName,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        PagedResult<Product> result = productService.getProducts(searchName, category, pageNumber, pageSize);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Integer id) {
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('Admin', 'ThuKho')")
    public ResponseEntity<Product> createProduct(@Valid @RequestBody ProductUpsertRequest request) {
        Product created = productService.createProduct(request);
        return ResponseEntity.created(URI.create("/api/inventory/product/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Admin', 'ThuKho')")
    public ResponseEntity<Void> updateProduct(
            @PathVariable Integer id,
            @Valid @RequestBody ProductUpsertRequest request
    ) {
        if (request.getId() != null && !id.equals(request.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID sản phẩm trong đường dẫn và nội dung không khớp.");
        }
        productService.updateProduct(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Admin', 'ThuKho')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Integer id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('Admin', 'ThuKho')")
    public ResponseEntity<UploadImageResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        UploadImageResponse response = productService.uploadProductImage(file);
        return ResponseEntity.ok(response);
    }
}
