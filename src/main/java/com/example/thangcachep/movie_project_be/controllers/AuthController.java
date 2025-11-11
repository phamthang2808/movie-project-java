package com.example.thangcachep.movie_project_be.controllers;


import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.thangcachep.movie_project_be.models.request.ForgotPasswordOtpRequest;
import com.example.thangcachep.movie_project_be.models.request.GoogleAuthRequest;
import com.example.thangcachep.movie_project_be.models.request.LoginRequest;
import com.example.thangcachep.movie_project_be.models.request.RegisterRequest;
import com.example.thangcachep.movie_project_be.models.request.ResetPasswordOtpRequest;
import com.example.thangcachep.movie_project_be.models.request.VerifyOtpRequest;
import com.example.thangcachep.movie_project_be.models.responses.AuthResponse;
import com.example.thangcachep.movie_project_be.services.impl.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // JWT is stateless, logout is handled on client side
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message", "Email xác thực thành công"));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody GoogleAuthRequest request) {
        try {
            AuthResponse response = authService.googleLogin(request.getCode());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RuntimeException("Google OAuth thất bại: " + e.getMessage());
        }
    }

    /**
     * Gửi OTP để đặt lại mật khẩu
     * POST /api/v1/auth/forgot-password-otp
     */
    @PostMapping("/forgot-password-otp")
    public ResponseEntity<Map<String, Object>> sendOtpForPasswordReset(
            @Valid @RequestBody ForgotPasswordOtpRequest request) {
        try {
            Map<String, Object> response = authService.sendOtpForPasswordReset(request.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Verify OTP (chỉ kiểm tra, không reset password)
     * POST /api/v1/auth/verify-otp
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtpOnly(
            @Valid @RequestBody VerifyOtpRequest request) {
        try {
            Map<String, Object> response = authService.verifyOtpOnly(
                    request.getEmail(),
                    request.getOtp()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Xác nhận OTP và đặt lại mật khẩu
     * POST /api/v1/auth/reset-password-otp
     * - Nếu OTP đã được verify trước đó (qua /verify-otp), có thể không gửi OTP (otp = null hoặc empty)
     * - Nếu OTP chưa được verify, cần gửi OTP để verify và reset cùng lúc
     */
    @PostMapping("/reset-password-otp")
    public ResponseEntity<Map<String, Object>> verifyOtpAndResetPassword(
            @Valid @RequestBody ResetPasswordOtpRequest request) {
        try {
            Map<String, Object> response = authService.verifyOtpAndResetPassword(
                    request.getEmail(),
                    request.getOtp(),
                    request.getNewPassword()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}


