package com.example.thangcachep.movie_project_be.services.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.thangcachep.movie_project_be.entities.RoleEntity;
import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.exceptions.DataNotFoundException;
import com.example.thangcachep.movie_project_be.models.request.ChangePasswordRequest;
import com.example.thangcachep.movie_project_be.models.responses.UserResponse;
import com.example.thangcachep.movie_project_be.repositories.RoleRepository;
import com.example.thangcachep.movie_project_be.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
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

    /**
     * Lấy danh sách tất cả users (admin only)
     * @param search Từ khóa tìm kiếm (name hoặc email)
     * @param roleName Lọc theo role (ADMIN, STAFF, USER)
     * @return Danh sách UserResponse
     * Cache: 15 phút, cache key dựa trên search và roleName
     */
    @Cacheable(value = "users", key = "'admin:users:all:' + (#search != null ? #search : '') + ':' + (#roleName != null ? #roleName : 'all')")
    public List<UserResponse> getAllUsers(String search, String roleName) {
        List<UserEntity> users;

        // Normalize search và roleName
        String normalizedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String normalizedRole = (roleName != null && !roleName.trim().isEmpty()) ? roleName.trim() : null;

        if (normalizedSearch != null && normalizedRole != null) {
            // Search và filter theo role
            users = userRepository.searchUsersByRole(normalizedSearch, normalizedRole);
        } else if (normalizedSearch != null) {
            // Chỉ search
            users = userRepository.searchUsers(normalizedSearch);
        } else if (normalizedRole != null) {
            // Chỉ filter theo role
            users = userRepository.findByRoleNameIgnoreCase(normalizedRole);
        } else {
            // Lấy tất cả
            users = userRepository.findAll();
        }

        return users.stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy user by ID
     * @param userId ID của user
     * @return UserResponse
     * @throws DataNotFoundException nếu không tìm thấy user
     */
    public UserResponse getUserById(Long userId) throws DataNotFoundException {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy user với ID: " + userId));
        return mapToUserResponse(user);
    }

    /**
     * Admin update user (có thể update role, balance, isActive, etc.)
     * @param userId ID của user cần update
     * @param request UserResponse chứa thông tin cần update
     * @param currentUser User hiện tại đang thực hiện action (để check quyền)
     * @return UserResponse đã được update
     * @throws DataNotFoundException nếu không tìm thấy user
     * @throws RuntimeException nếu không có quyền (ví dụ: xóa admin, xóa chính mình)
     */
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public UserResponse updateUser(Long userId, UserResponse request, UserEntity currentUser) throws DataNotFoundException {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy user với ID: " + userId));

        // Không cho phép update admin (trừ khi là chính admin đó update profile của mình)
        if (user.getRole().getName().equalsIgnoreCase("ADMIN") && !user.getId().equals(currentUser.getId())) {
            throw new RuntimeException("Không thể cập nhật thông tin của admin khác");
        }

        // Update name
        if (request.getName() != null) {
            user.setName(request.getName());
        }

        // Update phone
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        } else if (request.getPhone() == null) {
            user.setPhone(null);
        }

        // Update birthday
        user.setBirthday(request.getBirthday());

        // Update balance (admin only)
        if (request.getBalance() != null) {
            user.setBalance(request.getBalance());
        }

        // Update isActive (admin only)
        if (request.getIsActive() != null) {
            // Không cho phép ban admin
            if (user.getRole().getName().equalsIgnoreCase("ADMIN") && !request.getIsActive()) {
                throw new RuntimeException("Không thể khóa tài khoản admin");
            }
            user.setIsActive(request.getIsActive());
        }

        // Update role (admin only) - chỉ cho phép update USER và STAFF
        if (request.getRole() != null && !request.getRole().trim().isEmpty()) {
            // Không cho phép thay đổi role của admin
            if (user.getRole().getName().equalsIgnoreCase("ADMIN")) {
                throw new RuntimeException("Không thể thay đổi role của admin");
            }
            // Update role
            String newRoleName = request.getRole().trim().toUpperCase();
            RoleEntity newRole = roleRepository.findByName(newRoleName)
                    .orElseThrow(() -> new RuntimeException("Role không hợp lệ: " + newRoleName));
            user.setRole(newRole);
        }

        userRepository.save(user);
        return mapToUserResponse(user);
    }

    /**
     * Ban/Unban user
     * @param userId ID của user
     * @param currentUser User hiện tại đang thực hiện action
     * @return UserResponse đã được update
     * @throws DataNotFoundException nếu không tìm thấy user
     * @throws RuntimeException nếu không có quyền
     */
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public UserResponse banUser(Long userId, UserEntity currentUser) throws DataNotFoundException {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy user với ID: " + userId));

        // Không cho phép ban admin
        if (user.getRole().getName().equalsIgnoreCase("ADMIN")) {
            throw new RuntimeException("Không thể khóa tài khoản admin");
        }

        // Không cho phép ban chính mình
        if (user.getId().equals(currentUser.getId())) {
            throw new RuntimeException("Không thể khóa chính mình");
        }

        // Toggle isActive
        user.setIsActive(!user.getIsActive());
        userRepository.save(user);

        return mapToUserResponse(user);
    }

    /**
     * Delete user (hard delete)
     * @param userId ID của user
     * @param currentUser User hiện tại đang thực hiện action
     * @throws DataNotFoundException nếu không tìm thấy user
     * @throws RuntimeException nếu không có quyền
     */
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public void deleteUser(Long userId, UserEntity currentUser) throws DataNotFoundException {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy user với ID: " + userId));

        // Không cho phép xóa admin
        if (user.getRole().getName().equalsIgnoreCase("ADMIN")) {
            throw new RuntimeException("Không thể xóa tài khoản admin");
        }

        // Không cho phép xóa chính mình
        if (user.getId().equals(currentUser.getId())) {
            throw new RuntimeException("Không thể xóa chính mình");
        }

        // Hard delete
        userRepository.delete(user);
    }
}


