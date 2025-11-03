package com.example.thangcachep.movie_project_be.controllers;


import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.models.responses.UserResponse;
import com.example.thangcachep.movie_project_be.services.impl.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

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

    private UserEntity getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (UserEntity) authentication.getPrincipal();
    }
}


