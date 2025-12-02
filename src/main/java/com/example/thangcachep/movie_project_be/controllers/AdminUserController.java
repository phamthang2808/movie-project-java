package com.example.thangcachep.movie_project_be.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.exceptions.DataNotFoundException;
import com.example.thangcachep.movie_project_be.exceptions.PermissionDenyException;
import com.example.thangcachep.movie_project_be.models.responses.ApiResponse;
import com.example.thangcachep.movie_project_be.models.responses.UserResponse;
import com.example.thangcachep.movie_project_be.services.impl.UserService;

import lombok.RequiredArgsConstructor;

/**
 * Admin User Controller
 * Quyền hạn: Chỉ ADMIN mới có quyền quản lý users
 * Sử dụng @PreAuthorize để kiểm tra quyền truy cập
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')") // Chỉ ADMIN
public class AdminUserController {

    private final UserService userService;

    /**
     * Lấy danh sách tất cả users (admin only)
     * GET /api/v1/admin/users
     * Query params: search (tìm kiếm theo name/email), role (lọc theo role)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role) {
        List<UserResponse> users = userService.getAllUsers(search, role);
        ApiResponse<List<UserResponse>> response = ApiResponse.success("Lấy danh sách user thành công", users);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy user by ID
     * GET /api/v1/admin/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        ApiResponse<UserResponse> response = ApiResponse.success("Lấy user thành công", user);
        return ResponseEntity.ok(response);
    }

    /**
     * Update user (admin only)
     * PUT /api/v1/admin/users/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @RequestBody UserResponse request) {
        UserEntity currentUser = getCurrentUser();
        UserResponse updatedUser = userService.updateUser(id, request, currentUser);
        ApiResponse<UserResponse> response = ApiResponse.success("Cập nhật user thành công", updatedUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Ban/Unban user
     * PUT /api/v1/admin/users/{id}/ban
     */
    @PutMapping("/{id}/ban")
    public ResponseEntity<ApiResponse<UserResponse>> banUser(@PathVariable Long id) {
        UserEntity currentUser = getCurrentUser();
        UserResponse user = userService.banUser(id, currentUser);
        ApiResponse<UserResponse> response = ApiResponse.success("Cập nhật trạng thái user thành công", user);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete user (hard delete)
     * DELETE /api/v1/admin/users/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        UserEntity currentUser = getCurrentUser();
        userService.deleteUser(id, currentUser);
        ApiResponse<Void> response = ApiResponse.success("Xóa user thành công", null);
        return ResponseEntity.ok(response);
    }

    private UserEntity getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (UserEntity) authentication.getPrincipal();
    }
}

