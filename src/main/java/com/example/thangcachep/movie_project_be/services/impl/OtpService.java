package com.example.thangcachep.movie_project_be.services.impl;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.example.thangcachep.movie_project_be.models.dto.OtpData;

import lombok.extern.slf4j.Slf4j;

/**
 * OtpService - Quản lý OTP (One-Time Password)
 * Lưu OTP trong memory với thời gian hết hạn 10 phút
 */
@Service
@Slf4j
public class OtpService {

    // Lưu OTP: key = email, value = OtpData (otp, timestamp)
    private final Map<String, OtpData> otpStorage = new ConcurrentHashMap<>();

    // Thời gian hết hạn OTP: 10 phút (600000 milliseconds)
    private static final long OTP_EXPIRY_TIME = 10 * 60 * 1000;

    // Số lần thử tối đa cho mỗi email: 5 lần
    private static final int MAX_ATTEMPTS = 5;

    /**
     * Generate OTP 6 số
     */
    public String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // 6 số từ 100000 đến 999999
        return String.valueOf(otp);
    }

    /**
     * Lưu OTP cho email
     */
    public void saveOtp(String email, String otp) {
        OtpData otpData = new OtpData(otp, System.currentTimeMillis(), 0);
        otpStorage.put(email.toLowerCase(), otpData);
        log.info("✅ Đã lưu OTP cho email: {}", email);

        // Tự động xóa OTP hết hạn sau 10 phút
        scheduleCleanup(email);
    }

    /**
     * Verify OTP
     * @return true nếu OTP đúng và chưa hết hạn, false nếu sai hoặc hết hạn
     */
    public boolean verifyOtp(String email, String otp) {
        OtpData otpData = otpStorage.get(email.toLowerCase());

        if (otpData == null) {
            log.warn("❌ Không tìm thấy OTP cho email: {}", email);
            return false;
        }

        // Kiểm tra số lần thử
        if (otpData.getAttempts() >= MAX_ATTEMPTS) {
            log.warn("❌ Đã vượt quá số lần thử tối đa cho email: {}", email);
            otpStorage.remove(email.toLowerCase());
            return false;
        }

        // Kiểm tra thời gian hết hạn
        long currentTime = System.currentTimeMillis();
        if (currentTime - otpData.getTimestamp() > OTP_EXPIRY_TIME) {
            log.warn("❌ OTP đã hết hạn cho email: {}", email);
            otpStorage.remove(email.toLowerCase());
            return false;
        }

        // Kiểm tra OTP
        if (!otpData.getOtp().equals(otp)) {
            otpData.incrementAttempts();
            log.warn("❌ OTP không đúng cho email: {}, số lần thử: {}", email, otpData.getAttempts());
            return false;
        }

        // OTP đúng, xóa OTP sau khi verify thành công
        otpStorage.remove(email.toLowerCase());
        log.info("✅ OTP đã được xác nhận thành công cho email: {}", email);
        return true;
    }

    /**
     * Xóa OTP (sau khi đã sử dụng hoặc hết hạn)
     */
    public void removeOtp(String email) {
        otpStorage.remove(email.toLowerCase());
        log.info("✅ Đã xóa OTP cho email: {}", email);
    }

    /**
     * Kiểm tra xem email có OTP đang chờ không
     */
    public boolean hasOtp(String email) {
        OtpData otpData = otpStorage.get(email.toLowerCase());
        if (otpData == null) {
            return false;
        }

        // Kiểm tra thời gian hết hạn
        long currentTime = System.currentTimeMillis();
        if (currentTime - otpData.getTimestamp() > OTP_EXPIRY_TIME) {
            otpStorage.remove(email.toLowerCase());
            return false;
        }

        return true;
    }

    /**
     * Lên lịch xóa OTP sau khi hết hạn
     */
    private void scheduleCleanup(String email) {
        new Thread(() -> {
            try {
                Thread.sleep(OTP_EXPIRY_TIME);
                OtpData otpData = otpStorage.get(email.toLowerCase());
                if (otpData != null) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - otpData.getTimestamp() > OTP_EXPIRY_TIME) {
                        otpStorage.remove(email.toLowerCase());
                        log.info("✅ Đã tự động xóa OTP hết hạn cho email: {}", email);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}

