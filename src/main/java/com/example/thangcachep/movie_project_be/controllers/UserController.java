package com.example.thangcachep.movie_project_be.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
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

import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.models.responses.UserResponse;
import com.example.thangcachep.movie_project_be.repositories.UserRepository;
import com.example.thangcachep.movie_project_be.services.impl.FileStorageService;
import com.example.thangcachep.movie_project_be.services.impl.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile() {
        UserEntity user = getCurrentUser();
        // mapToUserResponse sẽ tự động check VIP expiration và trả về isVip chính xác
        // Nếu VIP đã hết hạn, nó sẽ được lưu vào DB trong checkAndUpdateVipExpiration
        UserResponse response = userService.mapToUserResponse(user);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(@RequestBody UserResponse request) {
        UserEntity user = getCurrentUser();
        UserResponse response = userService.updateProfile(user, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Upload avatar
     */
    @PostMapping("/upload-avatar")
    public ResponseEntity<Map<String, Object>> uploadAvatar(@RequestParam("avatar") MultipartFile file) {
        try {
            UserEntity user = getCurrentUser();

            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "File không được rỗng"));
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "File phải là ảnh"));
            }

            // Validate file size (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Ảnh không được vượt quá 5MB"));
            }

            // Upload file lên Cloudflare R2 (khuyến nghị cho production)
            // Hoặc có thể dùng uploadAvatarToLocal(file) để lưu local nếu cần
            String avatarUrl = fileStorageService.uploadFile(file);

            // Update user avatar
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);

            // Return response
            UserResponse userResponse = userService.mapToUserResponse(user);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("avatarUrl", avatarUrl);
            response.put("user", userResponse);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Upload avatar thất bại: " + e.getMessage()));
        }
    }

    private UserEntity getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (UserEntity) authentication.getPrincipal();
    }
}


