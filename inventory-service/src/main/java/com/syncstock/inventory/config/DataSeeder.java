package com.syncstock.inventory.config;

import com.syncstock.inventory.entity.Category;
import com.syncstock.inventory.entity.Product;
import com.syncstock.inventory.repository.CategoryRepository;
import com.syncstock.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seedCategories();
        seedProducts();
    }

    private void seedCategories() {
        if (categoryRepository.count() > 0) {
            log.info("Categories already exist, skipping seeding.");
            return;
        }

        List<Category> categories = List.of(
                Category.builder().name("Điện tử").build(),
                Category.builder().name("Gia dụng").build(),
                Category.builder().name("Thực phẩm").build()
        );

        categoryRepository.saveAll(categories);
        log.info("Successfully seeded {} categories.", categories.size());
    }

    private void seedProducts() {
        if (productRepository.count() > 0) {
            log.info("Products already exist, skipping seeding.");
            return;
        }

        List<Product> products = List.of(
                Product.builder().name("Laptop Dell XPS 15").category("Điện tử").unit("Cái").stock(50).minStock(10).build(),
                Product.builder().name("Bàn phím cơ Logitech").category("Điện tử").unit("Cái").stock(120).minStock(20).build(),
                Product.builder().name("Nồi chiên không dầu").category("Gia dụng").unit("Cái").stock(30).minStock(5).build(),
                Product.builder().name("Nước mắm Nam Ngư").category("Thực phẩm").unit("Chai").stock(500).minStock(50).build(),
                Product.builder().name("Chuột không dây Microsoft").category("Điện tử").unit("Cái").stock(85).minStock(15).build(),
                Product.builder().name("Tai nghe Sony WH-CH520").category("Điện tử").unit("Cái").stock(40).minStock(8).build(),
                Product.builder().name("Sạc dự phòng Anker 20000mAh").category("Điện tử").unit("Cái").stock(65).minStock(12).build(),
                Product.builder().name("Ấm siêu tốc Philips").category("Gia dụng").unit("Cái").stock(55).minStock(10).build(),
                Product.builder().name("Máy hút bụi cầm tay").category("Gia dụng").unit("Cái").stock(22).minStock(5).build(),
                Product.builder().name("Nồi cơm điện Sharp").category("Gia dụng").unit("Cái").stock(48).minStock(8).build(),
                Product.builder().name("Bột giặt OMO 3kg").category("Gia dụng").unit("Gói").stock(140).minStock(25).build(),
                Product.builder().name("Mì ăn liền Hảo Hảo").category("Thực phẩm").unit("Thùng").stock(260).minStock(40).build(),
                Product.builder().name("Gạo ST25").category("Thực phẩm").unit("Bao").stock(180).minStock(30).build(),
                Product.builder().name("Dầu ăn Neptune").category("Thực phẩm").unit("Chai").stock(210).minStock(35).build(),
                Product.builder().name("Sữa tươi Vinamilk").category("Thực phẩm").unit("Lốc").stock(320).minStock(60).build(),
                Product.builder().name("Đường Biên Hòa").category("Thực phẩm").unit("Kg").stock(175).minStock(25).build(),
                Product.builder().name("Nước rửa chén Sunlight").category("Gia dụng").unit("Chai").stock(110).minStock(20).build(),
                Product.builder().name("Bóng đèn LED Rạng Đông").category("Điện tử").unit("Bóng").stock(95).minStock(18).build(),
                Product.builder().name("Ổ cắm điện Lioa").category("Điện tử").unit("Cái").stock(70).minStock(12).build(),
                Product.builder().name("Khăn giấy Pulppy").category("Gia dụng").unit("Gói").stock(240).minStock(40).build()
        );

        productRepository.saveAll(products);
        log.info("Successfully seeded {} sample products.", products.size());
    }
}
