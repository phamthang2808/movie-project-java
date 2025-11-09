package com.example.thangcachep.movie_project_be.services.impl;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.models.request.ChangePasswordRequest;
import com.example.thangcachep.movie_project_be.models.responses.UserResponse;
import com.example.thangcachep.movie_project_be.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
        // Email không cho phép thay đổi qua profile update
        // if (request.getEmail() != null) {
        //     user.setEmail(request.getEmail());
        // }

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        } else if (request.getPhone() == null && user.getPhone() != null) {
            // Cho phép xóa phone nếu gửi null
            user.setPhone(null);
        }

        // Birthday - luôn update nếu có trong request (kể cả null để xóa)
        user.setBirthday(request.getBirthday());

        // Lưu user sau khi update
        userRepository.save(user);

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
                .phone(user.getPhone())
                .birthday(user.getBirthday())
                .build();
    }

    /**
     * Đổi mật khẩu của user
     * @param user UserEntity cần đổi mật khẩu
     * @param request ChangePasswordRequest chứa oldPassword, newPassword, confirmPassword
     * @throws IllegalArgumentException nếu mật khẩu cũ không đúng hoặc mật khẩu mới không khớp
     */
    @Transactional
    public void changePassword(UserEntity user, ChangePasswordRequest request) {
        // Kiểm tra mật khẩu cũ có đúng không
        // Nếu user đăng nhập bằng Google (không có password), bỏ qua check mật khẩu cũ
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                throw new IllegalArgumentException("Mật khẩu cũ không đúng");
            }
        }

        // Kiểm tra mật khẩu mới và xác nhận mật khẩu có khớp không
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới và xác nhận mật khẩu không khớp");
        }

        // Kiểm tra mật khẩu mới có khác mật khẩu cũ không
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
                throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu cũ");
            }
        }

        // Mã hóa mật khẩu mới và lưu vào database
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}


