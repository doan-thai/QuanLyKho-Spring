package com.syncstock.inventory.service;

import com.syncstock.inventory.dto.PagedResult;
import com.syncstock.inventory.dto.ProductUpsertRequest;
import com.syncstock.inventory.dto.UploadImageResponse;
import com.syncstock.inventory.entity.Product;
import com.syncstock.inventory.repository.ProductRepository;
import com.syncstock.inventory.repository.StockTransactionRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final StockTransactionRepository stockTransactionRepository;

    @Value("${file.upload-dir:uploads/images}")
    private String uploadDir;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    @Transactional(readOnly = true)
    public PagedResult<Product> getProducts(String searchName, String category, int pageNumber, int pageSize) {
        int safePageNumber = Math.max(1, pageNumber);
        int safePageSize = Math.max(1, Math.min(pageSize, 500));

        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (searchName != null && !searchName.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + searchName.trim().toLowerCase() + "%"));
            }

            if (category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("Tất cả")) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(safePageNumber - 1, safePageSize, Sort.by(Sort.Direction.ASC, "id"));
        Page<Product> page = productRepository.findAll(spec, pageable);

        int totalPages = page.getTotalPages();
        if (page.getTotalElements() == 0) {
            totalPages = 0;
        }

        return PagedResult.<Product>builder()
                .totalRecords(page.getTotalElements())
                .pageNumber(safePageNumber)
                .pageSize(safePageSize)
                .totalPages(totalPages)
                .data(page.getContent())
                .build();
    }

    @Transactional(readOnly = true)
    public Product getProductById(Integer id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm với ID: " + id));
    }

    @Transactional
    public Product createProduct(ProductUpsertRequest request) {
        Product product = Product.builder()
                .name(request.getName().trim())
                .category(request.getCategory().trim())
                .unit(request.getUnit().trim())
                .stock(Math.max(0, request.getStock() != null ? request.getStock() : 0))
                .minStock(Math.max(0, request.getMinStock() != null ? request.getMinStock() : 10))
                .imageUrl(request.getImageUrl() != null && !request.getImageUrl().trim().isEmpty() ? request.getImageUrl().trim() : null)
                .build();

        return productRepository.save(product);
    }

    @Transactional
    public void updateProduct(Integer id, ProductUpsertRequest request) {
        Product product = getProductById(id);

        product.setName(request.getName().trim());
        product.setCategory(request.getCategory().trim());
        product.setUnit(request.getUnit().trim());
        // Bảo vệ tính toàn vẹn: nếu sản phẩm đã có lịch sử giao dịch thì giữ nguyên stock
        // (stock chỉ được thay đổi qua phiếu nhập/xuất kho).
        // Nếu chưa có giao dịch nào, cho phép đặt lại stock ban đầu.
        boolean hasTransactions = stockTransactionRepository.existsByProductId(id);
        if (!hasTransactions) {
            product.setStock(Math.max(0, request.getStock() != null ? request.getStock() : 0));
        }
        product.setMinStock(Math.max(0, request.getMinStock() != null ? request.getMinStock() : 10));
        product.setImageUrl(request.getImageUrl() != null && !request.getImageUrl().trim().isEmpty() ? request.getImageUrl().trim() : null);

        productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Integer id) {
        Product product = getProductById(id);
        if (stockTransactionRepository.existsByProductId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không thể xóa sản phẩm đã phát sinh lịch sử giao dịch nhập/xuất kho."
            );
        }
        productRepository.delete(product);
    }

    public UploadImageResponse uploadProductImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không có file nào được chọn hoặc file rỗng.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kích thước file vượt quá giới hạn 5MB.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên file không hợp lệ.");
        }

        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalFilename.substring(dotIndex).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Định dạng ảnh không hợp lệ. Chỉ chấp nhận .jpg, .jpeg, .png, .webp.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content-Type không hợp lệ cho file ảnh.");
        }

        try {
            Path targetDir = Paths.get(uploadDir).toAbsolutePath().normalize();
            File dirFile = targetDir.toFile();
            if (!dirFile.exists()) {
                dirFile.mkdirs();
            }

            String uniqueFileName = UUID.randomUUID().toString().replace("-", "") + extension;
            Path targetPath = targetDir.resolve(uniqueFileName);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            String imageUrl = "/images/" + uniqueFileName;
            log.info("Saved product image successfully at {}", targetPath);

            return UploadImageResponse.builder()
                    .url(imageUrl)
                    .message("Upload ảnh thành công!")
                    .build();
        } catch (IOException ex) {
            log.error("Failed to store image file: ", ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lưu file ảnh: " + ex.getMessage());
        }
    }
}
