package com.syncstock.auth.config;

import com.syncstock.auth.entity.User;
import com.syncstock.auth.repository.UserRepository;
import com.syncstock.common.security.SecurityConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedDefaultAdmin();
    }

    private void seedDefaultAdmin() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .passwordHash(passwordEncoder.encode("Admin@123"))
                    .role(SecurityConstants.ROLE_ADMIN)
                    .isActive(true)
                    .build();
            userRepository.save(admin);
            log.info("Default Admin account seeded successfully: admin / Admin@123 (role: Admin)");
        }
    }
}
