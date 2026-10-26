package com.syncstock.auth.service;

import com.syncstock.auth.dto.*;
import com.syncstock.auth.entity.User;
import com.syncstock.auth.repository.UserRepository;
import com.syncstock.common.exception.AppException;
import com.syncstock.common.security.JwtUtils;
import com.syncstock.common.security.SecurityConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String FAILED_LOGIN_SESSION_KEY = "FailedLoginCount";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();

        if (userRepository.existsByUsername(username)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Tên đăng nhập đã tồn tại.");
        }

        boolean hasAdmin = userRepository.existsByRole(SecurityConstants.ROLE_ADMIN);
        String role;

        if (!hasAdmin) {
            role = SecurityConstants.ROLE_ADMIN;
            log.info("Chưa có Admin nào trong hệ thống. Đăng ký tài khoản '{}' với quyền Admin.", username);
        } else {
            if (request.getRole() != null && !request.getRole().isBlank()
                    && !SecurityConstants.ROLE_THUKHO.equalsIgnoreCase(request.getRole().trim())) {
                throw new AppException(HttpStatus.BAD_REQUEST,
                        "Hệ thống đã có Admin, chỉ cho phép tự đăng ký tài khoản với vai trò ThuKho.");
            }
            role = SecurityConstants.ROLE_THUKHO;
        }

        String passwordHash = passwordEncoder.encode(request.getPassword().trim());

        User user = User.builder()
                .username(username)
                .passwordHash(passwordHash)
                .role(role)
                .isActive(true)
                .build();

        user = userRepository.save(user);

        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getIsActive()
        );

        return new RegisterResponse("Đăng ký thành công", userResponse);
    }

    public ResponseEntity<?> login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String username = request.getUsername().trim();
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            int failed = incrementFailedLoginCount(httpRequest);
            log.warn("Đăng nhập thất bại: người dùng '{}' không tồn tại. Lần thất bại trong session: {}", username, failed);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginErrorResponse("Sai username hoặc password", failed));
        }

        User user = userOpt.get();

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            log.warn("Đăng nhập thất bại: tài khoản '{}' đã bị khóa.", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Tài khoản đã bị khóa."));
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int failed = incrementFailedLoginCount(httpRequest);
            log.warn("Đăng nhập thất bại: sai mật khẩu cho '{}'. Lần thất bại trong session: {}", username, failed);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginErrorResponse("Sai username hoặc password", failed));
        }

        resetFailedLoginCount(httpRequest);

        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        ResponseCookie cookie = ResponseCookie.from(SecurityConstants.COOKIE_NAME, token)
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofMinutes(60))
                .sameSite("Lax")
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        log.info("Đăng nhập thành công cho người dùng '{}' (Role: {}).", username, user.getRole());
        return ResponseEntity.ok(new LoginResponse(token, user.getUsername(), user.getRole(), 0));
    }

    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        ResponseCookie cookie = ResponseCookie.from(SecurityConstants.COOKIE_NAME, "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private int incrementFailedLoginCount(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(true);
        Integer current = (Integer) session.getAttribute(FAILED_LOGIN_SESSION_KEY);
        int failed = (current == null ? 0 : current) + 1;
        session.setAttribute(FAILED_LOGIN_SESSION_KEY, failed);
        return failed;
    }

    private void resetFailedLoginCount(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.setAttribute(FAILED_LOGIN_SESSION_KEY, 0);
        }
    }
}
