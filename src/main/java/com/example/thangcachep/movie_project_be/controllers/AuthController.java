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

import com.example.thangcachep.movie_project_be.components.LocalizationUtils;
import com.example.thangcachep.movie_project_be.models.request.ForgotPasswordOtpRequest;
import com.example.thangcachep.movie_project_be.models.request.GoogleAuthRequest;
import com.example.thangcachep.movie_project_be.models.request.LoginRequest;
import com.example.thangcachep.movie_project_be.models.request.RegisterRequest;
import com.example.thangcachep.movie_project_be.models.request.ResetPasswordOtpRequest;
import com.example.thangcachep.movie_project_be.models.request.VerifyOtpRequest;
import com.example.thangcachep.movie_project_be.models.responses.ApiResponse;
import com.example.thangcachep.movie_project_be.models.responses.AuthResponse;
import com.example.thangcachep.movie_project_be.services.impl.AuthService;
import com.example.thangcachep.movie_project_be.utils.MessageKeys;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;
    private final LocalizationUtils localizationUtils;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse authData = authService.register(request);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.REGISTER_SUCCESS);
        ApiResponse<AuthResponse> response = ApiResponse.success(message, 201, authData);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authData = authService.login(request);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.LOGIN_SUCCESS);
        ApiResponse<AuthResponse> response = ApiResponse.success(message, authData);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        // JWT is stateless, logout is handled on client side
        String message = localizationUtils.getLocalizedMessage(MessageKeys.LOGOUT_SUCCESS);
        ApiResponse<Void> response = ApiResponse.success(message, null);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.EMAIL_VERIFY_SUCCESS);
        ApiResponse<Void> response = ApiResponse.success(message, null);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(@Valid @RequestBody GoogleAuthRequest request) {
        AuthResponse authData = authService.googleLogin(request.getCode());
        String message = localizationUtils.getLocalizedMessage(MessageKeys.GOOGLE_LOGIN_SUCCESS);
        ApiResponse<AuthResponse> response = ApiResponse.success(message, authData);
        return ResponseEntity.ok(response);
    }

    /**
     * Gửi OTP để đặt lại mật khẩu
     * POST /api/v1/auth/forgot-password-otp
     */
    @PostMapping("/forgot-password-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendOtpForPasswordReset(
            @Valid @RequestBody ForgotPasswordOtpRequest request) {
        Map<String, Object> data = authService.sendOtpForPasswordReset(request.getEmail());
        String message = localizationUtils.getLocalizedMessage(MessageKeys.OTP_SEND_SUCCESS);
        ApiResponse<Map<String, Object>> response = ApiResponse.success(message, data);
        return ResponseEntity.ok(response);
    }

    /**
     * Verify OTP (chỉ kiểm tra, không reset password)
     * POST /api/v1/auth/verify-otp
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyOtpOnly(
            @Valid @RequestBody VerifyOtpRequest request) {
        Map<String, Object> data = authService.verifyOtpOnly(
                request.getEmail(),
                request.getOtp()
        );
        String message = localizationUtils.getLocalizedMessage(MessageKeys.OTP_VERIFY_SUCCESS);
        ApiResponse<Map<String, Object>> response = ApiResponse.success(message, data);
        return ResponseEntity.ok(response);
    }

    /**
     * Xác nhận OTP và đặt lại mật khẩu
     * POST /api/v1/auth/reset-password-otp
     * - Nếu OTP đã được verify trước đó (qua /verify-otp), có thể không gửi OTP (otp = null hoặc empty)
     * - Nếu OTP chưa được verify, cần gửi OTP để verify và reset cùng lúc
     */
    @PostMapping("/reset-password-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyOtpAndResetPassword(
            @Valid @RequestBody ResetPasswordOtpRequest request) {
        Map<String, Object> data = authService.verifyOtpAndResetPassword(
                request.getEmail(),
                request.getOtp(),
                request.getNewPassword()
        );
        String message = localizationUtils.getLocalizedMessage(MessageKeys.PASSWORD_RESET_SUCCESS);
        ApiResponse<Map<String, Object>> response = ApiResponse.success(message, data);
        return ResponseEntity.ok(response);
    }
}


