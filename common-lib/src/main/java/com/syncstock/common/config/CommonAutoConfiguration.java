package com.syncstock.common.config;

import com.syncstock.common.exception.GlobalExceptionHandler;
import com.syncstock.common.security.JwtAuthenticationFilter;
import com.syncstock.common.security.JwtUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Auto-Configuration cho common-lib.
 * Sử dụng @Import thay vì khởi tạo @Bean thủ công để đảm bảo Spring
 * tự quản lý vòng đời Bean và tiêm @Value từ application.yml (jwt.secret,
 * jwt.expiration-ms, ...) vào JwtUtils đúng cách — tránh bị override bởi
 * các hằng số DEFAULT_* cứng trong code.
 */
@AutoConfiguration
@Import({JwtUtils.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
public class CommonAutoConfiguration {
    // Không cần khai báo @Bean ở đây.
    // @Import đăng ký các class trực tiếp như Spring-managed components,
    // cho phép @Value và @Autowired trong JwtUtils hoạt động đầy đủ.
}
