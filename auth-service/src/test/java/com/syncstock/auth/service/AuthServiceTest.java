package com.syncstock.auth.service;

import com.syncstock.auth.dto.*;
import com.syncstock.auth.entity.User;
import com.syncstock.auth.repository.UserRepository;
import com.syncstock.common.exception.AppException;
import com.syncstock.common.security.JwtUtils;
import com.syncstock.common.security.SecurityConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private HttpServletResponse httpResponse;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1)
                .username("admin")
                .passwordHash("encodedPassword")
                .role("Admin")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Đăng ký tài khoản đầu tiên khi DB chưa có Admin -> Bắt buộc có role Admin")
    void register_FirstUserWithoutAdmin_ShouldAssignAdminRole() {
        RegisterRequest request = RegisterRequest.builder()
                .username("firstadmin")
                .password("password123")
                .role("ThuKho") // cố tình gửi ThuKho nhưng DB chưa có Admin -> vẫn gán Admin
                .build();

        when(userRepository.existsByUsername("firstadmin")).thenReturn(false);
        when(userRepository.existsByRole(SecurityConstants.ROLE_ADMIN)).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1);
            return u;
        });

        RegisterResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("Đăng ký thành công", response.getMessage());
        assertEquals(SecurityConstants.ROLE_ADMIN, response.getUser().getRole());
        assertEquals("firstadmin", response.getUser().getUsername());
    }

    @Test
    @DisplayName("Đăng ký khi DB đã có Admin -> Cho phép tự đăng ký với role ThuKho")
    void register_WhenAdminExists_ShouldAssignThuKhoRole() {
        RegisterRequest request = RegisterRequest.builder()
                .username("thukho1")
                .password("password123")
                .role("ThuKho")
                .build();

        when(userRepository.existsByUsername("thukho1")).thenReturn(false);
        when(userRepository.existsByRole(SecurityConstants.ROLE_ADMIN)).thenReturn(true);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(2);
            return u;
        });

        RegisterResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(SecurityConstants.ROLE_THUKHO, response.getUser().getRole());
    }

    @Test
    @DisplayName("Đăng ký khi DB đã có Admin nhưng cố tình đăng ký role khác ThuKho -> Ném ngoại lệ 400")
    void register_WhenAdminExistsAndRoleNotThuKho_ShouldThrowException() {
        RegisterRequest request = RegisterRequest.builder()
                .username("fakeadmin")
                .password("password123")
                .role("Admin")
                .build();

        when(userRepository.existsByUsername("fakeadmin")).thenReturn(false);
        when(userRepository.existsByRole(SecurityConstants.ROLE_ADMIN)).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> authService.register(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("chỉ cho phép tự đăng ký tài khoản với vai trò ThuKho"));
    }

    @Test
    @DisplayName("Đăng ký với username đã tồn tại -> Ném ngoại lệ 400")
    void register_DuplicateUsername_ShouldThrowException() {
        RegisterRequest request = RegisterRequest.builder()
                .username("existinguser")
                .password("password123")
                .build();

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> authService.register(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Tên đăng nhập đã tồn tại.", ex.getMessage());
    }

    @Test
    @DisplayName("Đăng nhập với tài khoản bị khóa -> Trả về 401 kèm thông báo tài khoản bị khóa")
    void login_LockedUser_ShouldReturn401() {
        User lockedUser = User.builder()
                .id(2)
                .username("lockeduser")
                .passwordHash("hash")
                .role("ThuKho")
                .isActive(false)
                .build();

        when(userRepository.findByUsername("lockeduser")).thenReturn(Optional.of(lockedUser));

        LoginRequest request = LoginRequest.builder()
                .username("lockeduser")
                .password("secret")
                .build();

        ResponseEntity<?> response = authService.login(request, httpRequest, httpResponse);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Tài khoản đã bị khóa.", body.get("message"));
    }

    @Test
    @DisplayName("Đăng nhập sai mật khẩu → Tăng biến đếm FailedLoginCount trong ConcurrentHashMap và trả về 401")
    void login_WrongPassword_ShouldIncrementFailedCountAndReturn401() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongpass", "encodedPassword")).thenReturn(false);

        LoginRequest request = LoginRequest.builder()
                .username("admin")
                .password("wrongpass")
                .build();

        // Đăng nhập thất bại lần đầu → count = 1
        ResponseEntity<?> response = authService.login(request, httpRequest, httpResponse);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody() instanceof LoginErrorResponse);
        LoginErrorResponse error = (LoginErrorResponse) response.getBody();
        assertEquals("Sai username hoặc password", error.getMessage());
        assertEquals(1, error.getFailedLoginCountInSession());

        // Đăng nhập thất bại lần hai → count = 2
        ResponseEntity<?> response2 = authService.login(request, httpRequest, httpResponse);
        LoginErrorResponse error2 = (LoginErrorResponse) response2.getBody();
        assertEquals(2, error2.getFailedLoginCountInSession());

        // Kiểm tra trực tiếp qua helper
        assertEquals(2, authService.getFailedLoginCount("admin"));
    }

    @Test
    @DisplayName("Đăng nhập đúng mật khẩu → Reset biến đếm FailedLoginCount, sinh JWT và gắn cookie")
    void login_CorrectPassword_ShouldReturnTokenAndSetCookie() {
        // Giả lập có 1 lần thất bại trước đó
        authService.incrementFailedLoginCount("admin");
        assertEquals(1, authService.getFailedLoginCount("admin"));

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("correctpass", "encodedPassword")).thenReturn(true);
        when(jwtUtils.generateToken(1, "admin", "Admin")).thenReturn("mock-jwt-token");

        LoginRequest request = LoginRequest.builder()
                .username("admin")
                .password("correctpass")
                .build();

        ResponseEntity<?> response = authService.login(request, httpRequest, httpResponse);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Sau khi login thành công, failedLoginCount phải bị xóa (reset về 0)
        assertEquals(0, authService.getFailedLoginCount("admin"));
        verify(httpResponse).addHeader(eq("Set-Cookie"), contains("access_token=mock-jwt-token"));

        assertTrue(response.getBody() instanceof LoginResponse);
        LoginResponse loginResponse = (LoginResponse) response.getBody();
        assertEquals("mock-jwt-token", loginResponse.getToken());
        assertEquals("admin", loginResponse.getUsername());
        assertEquals("Admin", loginResponse.getRole());
        assertEquals(0, loginResponse.getFailedLoginCountInSession());
    }
}
