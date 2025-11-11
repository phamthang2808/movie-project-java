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

    // Lưu trạng thái đã verify OTP: key = email, value = timestamp khi verify
    // Dùng để đánh dấu OTP đã được verify thành công (không xóa OTP ngay)
    private final Map<String, Long> verifiedEmails = new ConcurrentHashMap<>();

    // Thời gian hết hạn OTP: 10 phút (600000 milliseconds)
    private static final long OTP_EXPIRY_TIME = 10 * 60 * 1000;

    // Thời gian hết hạn cho verified email: 5 phút (300000 milliseconds)
    // Sau khi verify OTP thành công, user có 5 phút để reset password
    private static final long VERIFIED_EMAIL_EXPIRY_TIME = 5 * 60 * 1000;

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
     * Verify OTP (chỉ kiểm tra, không xóa OTP)
     * Dùng để verify OTP trước khi reset password
     * @return true nếu OTP đúng và chưa hết hạn, false nếu sai hoặc hết hạn
     */
    public boolean verifyOtpOnly(String email, String otp) {
        OtpData otpData = otpStorage.get(email.toLowerCase());

        if (otpData == null) {
            log.warn("❌ Không tìm thấy OTP cho email: {}", email);
            return false;
        }

        // Kiểm tra số lần thử
        if (otpData.getAttempts() >= MAX_ATTEMPTS) {
            log.warn("❌ Đã vượt quá số lần thử tối đa cho email: {}", email);
            otpStorage.remove(email.toLowerCase());
            verifiedEmails.remove(email.toLowerCase());
            return false;
        }

        // Kiểm tra thời gian hết hạn
        long currentTime = System.currentTimeMillis();
        if (currentTime - otpData.getTimestamp() > OTP_EXPIRY_TIME) {
            log.warn("❌ OTP đã hết hạn cho email: {}", email);
            otpStorage.remove(email.toLowerCase());
            verifiedEmails.remove(email.toLowerCase());
            return false;
        }

        // Kiểm tra OTP
        if (!otpData.getOtp().equals(otp)) {
            otpData.incrementAttempts();
            log.warn("❌ OTP không đúng cho email: {}, số lần thử: {}", email, otpData.getAttempts());
            return false;
        }

        // OTP đúng, đánh dấu email đã verify (không xóa OTP ngay)
        verifiedEmails.put(email.toLowerCase(), currentTime);
        log.info("✅ OTP đã được xác nhận thành công cho email: {} (chưa xóa OTP)", email);
        return true;
    }

    /**
     * Verify OTP và xóa OTP sau khi verify thành công
     * Dùng cho flow cũ (verify và reset password cùng lúc)
     * @return true nếu OTP đúng và chưa hết hạn, false nếu sai hoặc hết hạn
     */
    public boolean verifyOtp(String email, String otp) {
        // Nếu đã verify trước đó (trong 5 phút), cho phép reset password
        if (isOtpVerified(email)) {
            log.info("✅ Email đã được verify OTP trước đó: {}", email);
            // Xóa OTP và verified status sau khi reset password
            otpStorage.remove(email.toLowerCase());
            verifiedEmails.remove(email.toLowerCase());
            return true;
        }

        // Nếu chưa verify, kiểm tra OTP như bình thường
        OtpData otpData = otpStorage.get(email.toLowerCase());

        if (otpData == null) {
            log.warn("❌ Không tìm thấy OTP cho email: {}", email);
            return false;
        }

        // Kiểm tra số lần thử
        if (otpData.getAttempts() >= MAX_ATTEMPTS) {
            log.warn("❌ Đã vượt quá số lần thử tối đa cho email: {}", email);
            otpStorage.remove(email.toLowerCase());
            verifiedEmails.remove(email.toLowerCase());
            return false;
        }

        // Kiểm tra thời gian hết hạn
        long currentTime = System.currentTimeMillis();
        if (currentTime - otpData.getTimestamp() > OTP_EXPIRY_TIME) {
            log.warn("❌ OTP đã hết hạn cho email: {}", email);
            otpStorage.remove(email.toLowerCase());
            verifiedEmails.remove(email.toLowerCase());
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
        verifiedEmails.remove(email.toLowerCase());
        log.info("✅ OTP đã được xác nhận thành công cho email: {}", email);
        return true;
    }

    /**
     * Xóa OTP và trạng thái verified (sau khi đã sử dụng hoặc hết hạn)
     */
    public void removeOtp(String email) {
        otpStorage.remove(email.toLowerCase());
        verifiedEmails.remove(email.toLowerCase());
        log.info("✅ Đã xóa OTP và trạng thái verified cho email: {}", email);
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
            verifiedEmails.remove(email.toLowerCase());
            return false;
        }

        return true;
    }

    /**
     * Kiểm tra xem OTP đã được verify chưa (trong vòng 5 phút)
     */
    public boolean isOtpVerified(String email) {
        Long verifiedTime = verifiedEmails.get(email.toLowerCase());
        if (verifiedTime == null) {
            return false;
        }

        // Kiểm tra thời gian hết hạn (5 phút)
        long currentTime = System.currentTimeMillis();
        if (currentTime - verifiedTime > VERIFIED_EMAIL_EXPIRY_TIME) {
            verifiedEmails.remove(email.toLowerCase());
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

