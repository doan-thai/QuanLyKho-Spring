package com.syncstock.inventory.service;

import com.syncstock.inventory.dto.PagedResult;
import com.syncstock.inventory.dto.ProductUpsertRequest;
import com.syncstock.inventory.dto.UploadImageResponse;
import com.syncstock.inventory.entity.Product;
import com.syncstock.inventory.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @TempDir
    Path tempUploadDir;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(productService, "uploadDir", tempUploadDir.toString());

        sampleProduct = Product.builder()
                .id(1)
                .name("Laptop Dell XPS 15")
                .category("Điện tử")
                .unit("Cái")
                .stock(50)
                .minStock(10)
                .imageUrl(null)
                .version(0L)
                .build();
    }

    @Test
    @DisplayName("getProducts: Trả về phân trang đúng chuẩn PagedResult { totalRecords, pageNumber, pageSize, totalPages, data }")
    void getProducts_ShouldReturnPagedResult() {
        Page<Product> page = new PageImpl<>(List.of(sampleProduct), Pageable.ofSize(10), 1);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PagedResult<Product> result = productService.getProducts("Dell", "Điện tử", 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalRecords());
        assertEquals(1, result.getPageNumber());
        assertEquals(10, result.getPageSize());
        assertEquals(1, result.getTotalPages());
        assertEquals(1, result.getData().size());
        assertEquals("Laptop Dell XPS 15", result.getData().get(0).getName());
    }

    @Test
    @DisplayName("getProductById: Khi tìm thấy -> trả về Product")
    void getProductById_Found_ShouldReturnProduct() {
        when(productRepository.findById(1)).thenReturn(Optional.of(sampleProduct));

        Product product = productService.getProductById(1);

        assertNotNull(product);
        assertEquals(1, product.getId());
        assertEquals("Laptop Dell XPS 15", product.getName());
    }

    @Test
    @DisplayName("getProductById: Khi không tìm thấy -> ném lỗi 404")
    void getProductById_NotFound_ShouldThrow404() {
        when(productRepository.findById(999)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> productService.getProductById(999));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("createProduct: Lưu sản phẩm mới thành công")
    void createProduct_ShouldSaveAndReturnProduct() {
        ProductUpsertRequest request = ProductUpsertRequest.builder()
                .name("Bàn phím cơ Logitech")
                .category("Điện tử")
                .unit("Cái")
                .stock(100)
                .minStock(20)
                .imageUrl("/images/keyboard.png")
                .build();

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(2);
            return p;
        });

        Product created = productService.createProduct(request);

        assertNotNull(created);
        assertEquals(2, created.getId());
        assertEquals("Bàn phím cơ Logitech", created.getName());
        assertEquals(100, created.getStock());
    }

    @Test
    @DisplayName("updateProduct: Cập nhật thông tin sản phẩm thành công")
    void updateProduct_ShouldUpdateFields() {
        when(productRepository.findById(1)).thenReturn(Optional.of(sampleProduct));

        ProductUpsertRequest request = ProductUpsertRequest.builder()
                .id(1)
                .name("Laptop Dell XPS 15 Updated")
                .category("Điện tử")
                .unit("Cái")
                .stock(60)
                .minStock(15)
                .imageUrl("/images/new.png")
                .build();

        productService.updateProduct(1, request);

        verify(productRepository).save(sampleProduct);
        assertEquals("Laptop Dell XPS 15 Updated", sampleProduct.getName());
        assertEquals(60, sampleProduct.getStock());
        assertEquals(15, sampleProduct.getMinStock());
    }

    @Test
    @DisplayName("deleteProduct: Xóa sản phẩm thành công")
    void deleteProduct_ShouldDelete() {
        when(productRepository.findById(1)).thenReturn(Optional.of(sampleProduct));

        productService.deleteProduct(1);

        verify(productRepository).delete(sampleProduct);
    }

    @Test
    @DisplayName("uploadProductImage: File rỗng -> ném lỗi 400 Bad Request")
    void uploadImage_EmptyFile_ShouldThrow400() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.png", "image/png", new byte[0]);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> productService.uploadProductImage(emptyFile));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Không có file nào được chọn hoặc file rỗng."));
    }

    @Test
    @DisplayName("uploadProductImage: Kích thước vượt quá 5MB -> ném lỗi 400 Bad Request")
    void uploadImage_Exceeds5MB_ShouldThrow400() {
        byte[] largeBytes = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile largeFile = new MockMultipartFile("file", "large.png", "image/png", largeBytes);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> productService.uploadProductImage(largeFile));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Kích thước file vượt quá giới hạn 5MB."));
    }

    @Test
    @DisplayName("uploadProductImage: Định dạng không hợp lệ -> ném lỗi 400 Bad Request")
    void uploadImage_InvalidExtension_ShouldThrow400() {
        MockMultipartFile textFile = new MockMultipartFile("file", "script.exe", "application/octet-stream", "bad".getBytes());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> productService.uploadProductImage(textFile));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Định dạng ảnh không hợp lệ"));
    }

    @Test
    @DisplayName("uploadProductImage: File ảnh hợp lệ -> Lưu file và trả về URL")
    void uploadImage_ValidFile_ShouldSucceed() {
        MockMultipartFile validFile = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "image-content".getBytes());

        UploadImageResponse response = productService.uploadProductImage(validFile);

        assertNotNull(response);
        assertTrue(response.getUrl().startsWith("/images/"));
        assertTrue(response.getUrl().endsWith(".jpg"));
        assertEquals("Upload ảnh thành công!", response.getMessage());
    }
}
