package com.example.thangcachep.movie_project_be.services.impl;

import java.util.concurrent.CompletableFuture;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * EmailService - Xử lý gửi email
 * Sử dụng @Async để không block HTTP request
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Gửi email xác thực (Async - không block HTTP request)
     */
    @Async("emailExecutor")
    public CompletableFuture<Void> sendVerificationEmail(String toEmail, String token) {
        String verificationUrl = baseUrl + "/verify-email?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Xác thực tài khoản Movie Project");
        message.setText(
                "Chào mừng bạn đến với Movie Project!\n\n" +
                        "Vui lòng click vào link sau để xác thực tài khoản của bạn:\n" +
                        verificationUrl + "\n\n" +
                        "Link có hiệu lực trong 24 giờ.\n\n" +
                        "Trân trọng,\n" +
                        "Đội ngũ Movie Project"
        );

        try {
            mailSender.send(message);
            log.info("✅ Đã gửi email xác thực đến: {} (async)", toEmail);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("❌ Lỗi khi gửi email đến: {}", toEmail, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Gửi email reset password (Async - không block HTTP request)
     */
    @Async("emailExecutor")
    public CompletableFuture<Void> sendResetPasswordEmail(String toEmail, String token) {
        String resetUrl = baseUrl + "/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Đặt lại mật khẩu Movie Project");
        message.setText(
                "Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.\n\n" +
                        "Click vào link sau để đặt lại mật khẩu:\n" +
                        resetUrl + "\n\n" +
                        "Link có hiệu lực trong 1 giờ.\n\n" +
                        "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.\n\n" +
                        "Trân trọng,\n" +
                        "Đội ngũ Movie Project"
        );

        try {
            mailSender.send(message);
            log.info("✅ Đã gửi email reset password đến: {} (async)", toEmail);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("❌ Lỗi khi gửi email reset password đến: {}", toEmail, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Gửi email OTP để đặt lại mật khẩu (Async - không block HTTP request)
     */
    @Async("emailExecutor")
    public CompletableFuture<Void> sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Mã OTP đặt lại mật khẩu - Movie Project");
        message.setText(
                "Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.\n\n" +
                        "Mã OTP của bạn là: " + otp + "\n\n" +
                        "Mã OTP có hiệu lực trong 10 phút.\n\n" +
                        "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.\n\n" +
                        "Lưu ý: Không chia sẻ mã OTP này với bất kỳ ai.\n\n" +
                        "Trân trọng,\n" +
                        "Đội ngũ Movie Project"
        );

        try {
            mailSender.send(message);
            log.info("✅ Đã gửi email OTP đến: {} (async)", toEmail);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("❌ Lỗi khi gửi email OTP đến: {}", toEmail, e);
            return CompletableFuture.failedFuture(
                    new RuntimeException("Không thể gửi email OTP: " + e.getMessage())
            );
        }
    }
}

