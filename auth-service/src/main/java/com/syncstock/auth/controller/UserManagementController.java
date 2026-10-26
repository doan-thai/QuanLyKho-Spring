package com.syncstock.auth.controller;

import com.syncstock.auth.dto.CreateManagedUserRequest;
import com.syncstock.auth.dto.UpdateRoleRequest;
import com.syncstock.auth.dto.UpdateStatusRequest;
import com.syncstock.auth.dto.UserResponse;
import com.syncstock.auth.service.UserService;
import com.syncstock.common.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth/users")
@PreAuthorize("hasAuthority('Admin')")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getUsers(@RequestParam(required = false) String role) {
        List<UserResponse> users = userService.getUsers(role);
        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateManagedUserRequest request) {
        UserResponse createdUser = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateRoleRequest request) {
        UserResponse updatedUser = userService.updateRole(id, request);
        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Integer currentUserId = principal != null ? principal.getUserId() : null;
        UserResponse updatedUser = userService.updateStatus(id, request, currentUserId);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Integer currentUserId = principal != null ? principal.getUserId() : null;
        userService.deleteUser(id, currentUserId);
        return ResponseEntity.noContent().build();
    }
}
