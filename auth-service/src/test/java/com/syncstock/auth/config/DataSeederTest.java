package com.syncstock.auth.config;

import com.syncstock.auth.entity.User;
import com.syncstock.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests cho DataSeeder — xác minh:
 * 1. resolveAdminPassword() dùng biến môi trường hoặc sinh random.
 * 2. Mật khẩu không được lưu dưới dạng plaintext.
 * 3. Không tạo admin lần hai nếu đã tồn tại.
 *
 * Dùng LENIENT để tránh UnnecessaryStubbing khi setUp() stub passwordEncoder
 * mà các test resolveAdminPassword() không cần encoder.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataSeeder dataSeeder;

    @BeforeEach
    void setUp() {
        // Trả về chuỗi hash giả — KHÔNG chứa plaintext của bất kỳ password nào.
        when(passwordEncoder.encode(anyString()))
                .thenReturn("$2a$10$hashed_value_does_not_contain_plaintext");
    }

    // -------------------------------------------------------------------------
    // resolveAdminPassword()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("resolveAdminPassword: ADMIN_INITIAL_PASSWORD hợp lệ (>= 8 ký tự) -> dùng giá trị đó")
    void resolveAdminPassword_ValidEnvVar_UsesProvidedPassword() {
        ReflectionTestUtils.setField(dataSeeder, "adminInitialPassword", "MyStr0ngP@ss");
        String result = dataSeeder.resolveAdminPassword();
        assertEquals("MyStr0ngP@ss", result);
    }

    @Test
    @DisplayName("resolveAdminPassword: ADMIN_INITIAL_PASSWORD rỗng -> sinh mật khẩu ngẫu nhiên đủ dài")
    void resolveAdminPassword_EmptyEnvVar_GeneratesRandom() {
        ReflectionTestUtils.setField(dataSeeder, "adminInitialPassword", "");
        String result = dataSeeder.resolveAdminPassword();
        assertNotNull(result);
        assertTrue(result.length() >= 16,
                "Random password should be >= 16 chars, got: " + result.length());
    }

    @Test
    @DisplayName("resolveAdminPassword: ADMIN_INITIAL_PASSWORD null -> sinh mật khẩu ngẫu nhiên")
    void resolveAdminPassword_NullEnvVar_GeneratesRandom() {
        ReflectionTestUtils.setField(dataSeeder, "adminInitialPassword", null);
        String result = dataSeeder.resolveAdminPassword();
        assertNotNull(result);
        assertTrue(result.length() >= 16);
    }

    @Test
    @DisplayName("resolveAdminPassword: ADMIN_INITIAL_PASSWORD quá ngắn (< 8 ký tự) -> sinh mật khẩu ngẫu nhiên")
    void resolveAdminPassword_TooShortEnvVar_GeneratesRandom() {
        ReflectionTestUtils.setField(dataSeeder, "adminInitialPassword", "short");
        String result = dataSeeder.resolveAdminPassword();
        assertNotNull(result);
        assertNotEquals("short", result);
        assertTrue(result.length() >= 16);
    }

    @Test
    @DisplayName("resolveAdminPassword: Hai lần gọi với password rỗng sinh ra chuỗi khác nhau (entropy)")
    void resolveAdminPassword_EmptyEnvVar_TwoCallsProduceDifferentPasswords() {
        ReflectionTestUtils.setField(dataSeeder, "adminInitialPassword", "");
        String first  = dataSeeder.resolveAdminPassword();
        String second = dataSeeder.resolveAdminPassword();
        assertNotEquals(first, second, "Two random passwords should be different");
    }

    // -------------------------------------------------------------------------
    // run() / seedDefaultAdmin()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("run: Admin chưa tồn tại -> lưu user mới, password được hash")
    void run_AdminNotExists_ShouldSaveAdmin() throws Exception {
        ReflectionTestUtils.setField(dataSeeder, "adminInitialPassword", "SecurePass123!");
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        dataSeeder.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertEquals("admin", saved.getUsername());
        // Hash stub không bằng plaintext
        assertNotEquals("SecurePass123!", saved.getPasswordHash());
        assertFalse(saved.getPasswordHash().contains("SecurePass123!"),
                "Plaintext must NOT appear in the hash");
        assertTrue(saved.getIsActive());
    }

    @Test
    @DisplayName("run: Admin đã tồn tại -> không lưu user mới")
    void run_AdminAlreadyExists_ShouldSkip() throws Exception {
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        dataSeeder.run();

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("run: Mật khẩu plaintext KHÔNG được lưu trong hash")
    void run_AdminNotExists_PasswordNotStoredInPlaintext() throws Exception {
        String plainPassword = "SuperSecret123!";
        ReflectionTestUtils.setField(dataSeeder, "adminInitialPassword", plainPassword);
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> dataSeeder.run());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertFalse(captor.getValue().getPasswordHash().contains(plainPassword),
                "Plaintext password must NOT appear in the stored hash");
    }
}
