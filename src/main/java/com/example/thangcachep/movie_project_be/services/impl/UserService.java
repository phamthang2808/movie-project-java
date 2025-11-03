package com.example.thangcachep.movie_project_be.services.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.models.responses.UserResponse;
import com.example.thangcachep.movie_project_be.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Kiểm tra và tự động cập nhật VIP status nếu đã hết hạn
     * @param user UserEntity cần kiểm tra
     * @return true nếu đã update (save vào DB), false nếu không cần update
     */
    @Transactional
    public boolean checkAndUpdateVipExpiration(UserEntity user) {
        // Chỉ check nếu user đang là VIP và có vipExpiredAt
        if (user.getIsVip() != null && user.getIsVip()
                && user.getVipExpiredAt() != null) {

            LocalDateTime now = LocalDateTime.now();

            // Nếu vipExpiredAt đã qua (hết hạn)
            if (user.getVipExpiredAt().isBefore(now)) {
                user.setIsVip(false);
                // Lưu vào database ngay lập tức
                userRepository.save(user);
                return true;
            }
        }
        // Nếu user không phải VIP hoặc isVip = false trong DB, đảm bảo isVip = false
        if (user.getIsVip() == null || !user.getIsVip()) {
            user.setIsVip(false);
        }
        return false;
    }

    @Transactional
    public UserResponse updateProfile(UserEntity user, UserResponse request) {
        // Check VIP expiration trước khi update
        checkAndUpdateVipExpiration(user);

        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        // Note: avatar update should be handled separately via upload endpoint

        return mapToUserResponse(user);
    }

    public UserResponse mapToUserResponse(UserEntity user) {
        // Tự động check VIP expiration trước khi trả về response
        // Đảm bảo luôn lấy giá trị isVip mới nhất từ database
        boolean wasUpdated = checkAndUpdateVipExpiration(user);

        // Nếu đã update, reload user từ DB để đảm bảo có data mới nhất
        if (wasUpdated) {
            user = userRepository.findById(user.getId()).orElse(user);
        }

        // Đảm bảo isVip chỉ là true nếu thực sự là VIP và chưa hết hạn
        boolean isActuallyVip = user.getIsVip() != null
                && user.getIsVip()
                && user.getVipExpiredAt() != null
                && user.getVipExpiredAt().isAfter(LocalDateTime.now());

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().getName())
                .balance(user.getBalance())
                .isVip(isActuallyVip) // Chỉ trả về true nếu thực sự là VIP
                .vipExpiredAt(user.getVipExpiredAt())
                .isEmailVerified(user.getIsEmailVerified())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}


