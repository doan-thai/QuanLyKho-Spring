package com.syncstock.auth.service;

import com.syncstock.auth.dto.CreateManagedUserRequest;
import com.syncstock.auth.dto.UpdateRoleRequest;
import com.syncstock.auth.dto.UpdateStatusRequest;
import com.syncstock.auth.dto.UserResponse;
import com.syncstock.auth.entity.User;
import com.syncstock.auth.repository.UserRepository;
import com.syncstock.common.exception.AppException;
import com.syncstock.common.security.SecurityConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> getUsers(String role) {
        List<User> users;
        if (role != null && !role.isBlank() && !"Tất cả".equalsIgnoreCase(role.trim())) {
            users = userRepository.findByRoleOrderByUsernameAsc(role.trim());
        } else {
            users = userRepository.findAllByOrderByUsernameAsc();
        }

        return users.stream()
                .map(this::toUserResponse)
                .toList();
    }

    @Transactional
    public UserResponse createUser(CreateManagedUserRequest request) {
        String username = request.getUsername().trim();

        if (userRepository.existsByUsername(username)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Tên đăng nhập đã tồn tại.");
        }

        String role = request.getRole().trim();
        if (!SecurityConstants.MANAGED_ROLES.contains(role)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Chỉ được phép tạo tài khoản với vai trò ThuKho hoặc KeToan.");
        }

        String passwordHash = passwordEncoder.encode(request.getPassword().trim());

        User user = User.builder()
                .username(username)
                .passwordHash(passwordHash)
                .role(role)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        user = userRepository.save(user);
        log.info("Admin đã tạo tài khoản mới: '{}' với vai trò: '{}'", username, role);

        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateRole(Integer id, UpdateRoleRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng với ID: " + id));

        if (SecurityConstants.ROLE_ADMIN.equalsIgnoreCase(user.getRole())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Không thể thay đổi vai trò của tài khoản Admin.");
        }

        String newRole = request.getRole().trim();
        if (!SecurityConstants.MANAGED_ROLES.contains(newRole)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Chỉ được phép đổi sang vai trò ThuKho hoặc KeToan.");
        }

        user.setRole(newRole);
        user = userRepository.save(user);
        log.info("Đã cập nhật vai trò cho người dùng ID {}: '{}' -> '{}'", id, user.getUsername(), newRole);

        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateStatus(Integer id, UpdateStatusRequest request, Integer currentUserId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng với ID: " + id));

        if (id.equals(currentUserId) && Boolean.FALSE.equals(request.getIsActive())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Không thể tự khóa tài khoản của chính mình.");
        }

        if (SecurityConstants.ROLE_ADMIN.equalsIgnoreCase(user.getRole())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Không thể thay đổi trạng thái của tài khoản Admin.");
        }

        user.setIsActive(request.getIsActive());
        user = userRepository.save(user);
        log.info("Đã cập nhật trạng thái cho người dùng ID {}: isActive={}", id, request.getIsActive());

        return toUserResponse(user);
    }

    @Transactional
    public void deleteUser(Integer id, Integer currentUserId) {
        if (id.equals(currentUserId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Không thể tự xóa tài khoản của chính mình.");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng với ID: " + id));

        if (SecurityConstants.ROLE_ADMIN.equalsIgnoreCase(user.getRole())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Không thể xóa tài khoản Admin.");
        }

        userRepository.delete(user);
        log.info("Admin đã xóa tài khoản ID {}: '{}'", id, user.getUsername());
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getIsActive()
        );
    }
}
