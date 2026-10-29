package com.syncstock.auth.config;

import com.syncstock.auth.entity.User;
import com.syncstock.auth.repository.UserRepository;
import com.syncstock.common.security.SecurityConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** Lấy từ biến môi trường ADMIN_INITIAL_PASSWORD; nếu rỗng sẽ sinh ngẫu nhiên. */
    @Value("${ADMIN_INITIAL_PASSWORD:}")
    private String adminInitialPassword;

    @Override
    @Transactional
    public void run(String... args) {
        seedDefaultAdmin();
    }

    private void seedDefaultAdmin() {
        if (!userRepository.existsByUsername("admin")) {
            String password = resolveAdminPassword();
            User admin = User.builder()
                    .username("admin")
                    .passwordHash(passwordEncoder.encode(password))
                    .role(SecurityConstants.ROLE_ADMIN)
                    .isActive(true)
                    .build();
            userRepository.save(admin);
            // KHÔNG in mật khẩu rõ vào log — chỉ thông báo trạng thái
            log.warn("[SECURITY] Default admin account created. " +
                     "If ADMIN_INITIAL_PASSWORD was not set, a random password was generated. " +
                     "Please set ADMIN_INITIAL_PASSWORD and restart, or change the password immediately via the UI.");
        }
    }

    /**
     * Trả về mật khẩu admin ban đầu.
     * Nếu biến môi trường ADMIN_INITIAL_PASSWORD được cấu hình và có độ dài ≥ 8 ký tự,
     * dùng giá trị đó; ngược lại sinh mật khẩu ngẫu nhiên an toàn và chỉ ghi nhật ký
     * cảnh báo (không in mật khẩu rõ).
     */
    String resolveAdminPassword() {
        if (adminInitialPassword != null && adminInitialPassword.length() >= 8) {
            return adminInitialPassword;
        }
        // Sinh mật khẩu ngẫu nhiên 24 byte (32 ký tự Base64 URL-safe)
        byte[] randomBytes = new byte[24];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
