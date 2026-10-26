package com.syncstock.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Auth Service - Entry Point.
 * Không cần khai báo scanBasePackages = "com.syncstock.common" vì
 * common-lib đã đăng ký AutoConfiguration qua META-INF/spring/
 * org.springframework.boot.autoconfigure.AutoConfiguration.imports —
 * Spring Boot tự động nạp mà không cần component scan thêm.
 */
@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
