package com.example.thangcachep.movie_project_be.models.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * OtpData - DTO để lưu thông tin OTP
 */
@Getter
@AllArgsConstructor
public class OtpData {
    private final String otp;
    private final long timestamp;
    private int attempts;

    /**
     * Tăng số lần thử
     */
    public void incrementAttempts() {
        this.attempts++;
    }
}

