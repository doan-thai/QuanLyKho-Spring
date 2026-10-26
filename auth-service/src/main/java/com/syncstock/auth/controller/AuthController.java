package com.syncstock.auth.controller;

import com.syncstock.auth.dto.LoginRequest;
import com.syncstock.auth.dto.RegisterRequest;
import com.syncstock.auth.dto.RegisterResponse;
import com.syncstock.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        return authService.login(request, httpRequest, httpResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        authService.logout(httpRequest, httpResponse);
        return ResponseEntity.ok(Map.of("message", "Đăng xuất thành công"));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<Map<String, String>> checkAdminProbe() {
        return ResponseEntity.ok(Map.of(
                "message", "Truy cập quyền Admin thành công",
                "role", "Admin"
        ));
    }

    @GetMapping("/warehouse")
    @PreAuthorize("hasAnyAuthority('Admin', 'ThuKho')")
    public ResponseEntity<Map<String, String>> checkWarehouseProbe() {
        return ResponseEntity.ok(Map.of(
                "message", "Truy cập quyền Kho thành công"
        ));
    }

    @GetMapping("/report")
    @PreAuthorize("hasAnyAuthority('Admin', 'KeToan')")
    public ResponseEntity<Map<String, String>> checkReportProbe() {
        return ResponseEntity.ok(Map.of(
                "message", "Truy cập quyền Báo cáo thành công"
        ));
    }
}
