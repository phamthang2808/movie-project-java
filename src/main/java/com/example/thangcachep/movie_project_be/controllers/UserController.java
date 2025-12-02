package com.example.thangcachep.movie_project_be.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.thangcachep.movie_project_be.components.LocalizationUtils;
import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.models.request.ChangePasswordRequest;
import com.example.thangcachep.movie_project_be.models.responses.ApiResponse;
import com.example.thangcachep.movie_project_be.models.responses.UserResponse;
import com.example.thangcachep.movie_project_be.repositories.UserRepository;
import com.example.thangcachep.movie_project_be.services.impl.AzureBlobStorageService;
import com.example.thangcachep.movie_project_be.services.impl.UserService;
import com.example.thangcachep.movie_project_be.utils.MessageKeys;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final AzureBlobStorageService azureBlobStorageService;
    private final LocalizationUtils localizationUtils;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile() {
        UserEntity user = getCurrentUser();
        // mapToUserResponse sẽ tự động check VIP expiration và trả về isVip chính xác
        // Nếu VIP đã hết hạn, nó sẽ được lưu vào DB trong checkAndUpdateVipExpiration
        UserResponse userData = userService.mapToUserResponse(user);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.USER_GET_PROFILE_SUCCESS);
        ApiResponse<UserResponse> response = ApiResponse.success(message, userData);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@RequestBody UserResponse request) {
        UserEntity user = getCurrentUser();
        UserResponse userData = userService.updateProfile(user, request);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.USER_UPDATE_PROFILE_SUCCESS);
        ApiResponse<UserResponse> response = ApiResponse.success(message, userData);
        return ResponseEntity.ok(response);
    }

    /**
     * Upload avatar
     */
    @PostMapping("/upload-avatar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadAvatar(@RequestParam("avatar") MultipartFile file) {
        UserEntity user = getCurrentUser();

        // Validate file
        if (file.isEmpty()) {
            ApiResponse<Map<String, Object>> response = ApiResponse.error("File không được rỗng", 400);
            return ResponseEntity.badRequest().body(response);
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            ApiResponse<Map<String, Object>> response = ApiResponse.error("File phải là ảnh", 400);
            return ResponseEntity.badRequest().body(response);
        }

        // Validate file size (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            ApiResponse<Map<String, Object>> response = ApiResponse.error("Ảnh không được vượt quá 5MB", 400);
            return ResponseEntity.badRequest().body(response);
        }

        // Upload file lên Azure Blob Storage
        String avatarUrl = azureBlobStorageService.uploadFile(file);

        // Update user avatar
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        // Return response
        UserResponse userResponse = userService.mapToUserResponse(user);
        Map<String, Object> data = new HashMap<>();
        data.put("avatarUrl", avatarUrl);
        data.put("user", userResponse);

        String message = localizationUtils.getLocalizedMessage(MessageKeys.USER_UPLOAD_AVATAR_SUCCESS);
        ApiResponse<Map<String, Object>> response = ApiResponse.success(message, data);
        return ResponseEntity.ok(response);
    }

    /**
     * Đổi mật khẩu
     * PUT /api/v1/users/change-password
     * Yêu cầu authentication
     */
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        UserEntity user = getCurrentUser();
        userService.changePassword(user, request);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.USER_CHANGE_PASSWORD_SUCCESS);
        ApiResponse<Void> response = ApiResponse.success(message, null);
        return ResponseEntity.ok(response);
    }

    private UserEntity getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (UserEntity) authentication.getPrincipal();
    }
}


