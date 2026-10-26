package com.syncstock.auth.service;

import com.syncstock.auth.dto.CreateManagedUserRequest;
import com.syncstock.auth.dto.UpdateRoleRequest;
import com.syncstock.auth.dto.UpdateStatusRequest;
import com.syncstock.auth.dto.UserResponse;
import com.syncstock.auth.entity.User;
import com.syncstock.auth.repository.UserRepository;
import com.syncstock.common.exception.AppException;
import com.syncstock.common.security.SecurityConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User adminUser;
    private User thukhoUser;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1)
                .username("admin")
                .passwordHash("hash")
                .role(SecurityConstants.ROLE_ADMIN)
                .isActive(true)
                .build();

        thukhoUser = User.builder()
                .id(2)
                .username("thukho1")
                .passwordHash("hash")
                .role(SecurityConstants.ROLE_THUKHO)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Lấy danh sách user sắp xếp theo username")
    void getUsers_All_ShouldReturnSortedList() {
        when(userRepository.findAllByOrderByUsernameAsc()).thenReturn(List.of(adminUser, thukhoUser));

        List<UserResponse> result = userService.getUsers(null);

        assertEquals(2, result.size());
        assertEquals("admin", result.get(0).getUsername());
        assertEquals("thukho1", result.get(1).getUsername());
    }

    @Test
    @DisplayName("Lọc danh sách user theo role")
    void getUsers_WithRoleFilter_ShouldFilterCorrectly() {
        when(userRepository.findByRoleOrderByUsernameAsc("ThuKho")).thenReturn(List.of(thukhoUser));

        List<UserResponse> result = userService.getUsers("ThuKho");

        assertEquals(1, result.size());
        assertEquals("thukho1", result.get(0).getUsername());
    }

    @Test
    @DisplayName("Tạo user mới với vai trò ThuKho thành công")
    void createUser_ValidRequest_ShouldCreateUser() {
        CreateManagedUserRequest request = CreateManagedUserRequest.builder()
                .username("newthukho")
                .password("password123")
                .role("ThuKho")
                .isActive(true)
                .build();

        when(userRepository.existsByUsername("newthukho")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(3);
            return u;
        });

        UserResponse response = userService.createUser(request);

        assertNotNull(response);
        assertEquals("newthukho", response.getUsername());
        assertEquals("ThuKho", response.getRole());
        assertTrue(response.getIsActive());
    }

    @Test
    @DisplayName("Tạo user với vai trò Admin -> Bị chặn với lỗi 400")
    void createUser_WithAdminRole_ShouldThrowException() {
        CreateManagedUserRequest request = CreateManagedUserRequest.builder()
                .username("newadmin")
                .password("password123")
                .role("Admin")
                .isActive(true)
                .build();

        when(userRepository.existsByUsername("newadmin")).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () -> userService.createUser(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Chỉ được phép tạo tài khoản với vai trò ThuKho hoặc KeToan"));
    }

    @Test
    @DisplayName("Cập nhật vai trò từ ThuKho sang KeToan")
    void updateRole_ValidRole_ShouldUpdateRole() {
        UpdateRoleRequest request = UpdateRoleRequest.builder()
                .role("KeToan")
                .build();

        when(userRepository.findById(2)).thenReturn(Optional.of(thukhoUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateRole(2, request);

        assertEquals("KeToan", response.getRole());
    }

    @Test
    @DisplayName("Không cho phép thay đổi vai trò của tài khoản Admin")
    void updateRole_AdminTarget_ShouldThrowException() {
        UpdateRoleRequest request = UpdateRoleRequest.builder()
                .role("ThuKho")
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(adminUser));

        AppException ex = assertThrows(AppException.class, () -> userService.updateRole(1, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Không thể thay đổi vai trò của tài khoản Admin"));
    }

    @Test
    @DisplayName("Khóa/mở khóa tài khoản thành công")
    void updateStatus_ShouldUpdateIsActive() {
        UpdateStatusRequest request = UpdateStatusRequest.builder()
                .isActive(false)
                .build();

        when(userRepository.findById(2)).thenReturn(Optional.of(thukhoUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateStatus(2, request, 1);

        assertFalse(response.getIsActive());
    }

    @Test
    @DisplayName("Admin không thể tự khóa tài khoản của chính mình")
    void updateStatus_SelfLock_ShouldThrowException() {
        UpdateStatusRequest request = UpdateStatusRequest.builder()
                .isActive(false)
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(adminUser));

        AppException ex = assertThrows(AppException.class, () -> userService.updateStatus(1, request, 1));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Không thể tự khóa tài khoản của chính mình"));
    }

    @Test
    @DisplayName("Admin xóa tài khoản người dùng khác thành công")
    void deleteUser_OtherUser_ShouldDelete() {
        when(userRepository.findById(2)).thenReturn(Optional.of(thukhoUser));

        userService.deleteUser(2, 1);

        verify(userRepository).delete(thukhoUser);
    }

    @Test
    @DisplayName("Admin không thể tự xóa tài khoản của chính mình")
    void deleteUser_SelfDelete_ShouldThrowException() {
        AppException ex = assertThrows(AppException.class, () -> userService.deleteUser(1, 1));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Không thể tự xóa tài khoản của chính mình"));
        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Không thể xóa tài khoản Admin khác")
    void deleteUser_AdminTarget_ShouldThrowException() {
        User anotherAdmin = User.builder()
                .id(99)
                .username("anotherAdmin")
                .role("Admin")
                .build();

        when(userRepository.findById(99)).thenReturn(Optional.of(anotherAdmin));

        AppException ex = assertThrows(AppException.class, () -> userService.deleteUser(99, 1));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Không thể xóa tài khoản Admin"));
        verify(userRepository, never()).delete(any());
    }
}
