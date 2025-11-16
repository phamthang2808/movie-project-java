package com.example.thangcachep.movie_project_be.services.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import jakarta.mail.internet.MimeMessage;

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

    // Cache templates để không phải đọc file mỗi lần
    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    /**
     * Gửi email xác thực (Async - không block HTTP request)
     */
    @Async("emailExecutor")
    public CompletableFuture<Void> sendVerificationEmail(String toEmail, String token) {
        String verificationUrl = baseUrl + "/verify-email?token=" + token;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(fromEmail, "Movie Project");
            helper.setTo(toEmail);
            helper.setSubject("Xác thực tài khoản Movie Project");

            // Đọc HTML template
            String htmlContent = loadEmailTemplate("templates/verification-email.html");
            // Thay thế placeholder
            htmlContent = htmlContent.replace("${verificationUrl}", verificationUrl);

            helper.setText(htmlContent, true); // true = HTML

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
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(fromEmail, "Movie Project");
            helper.setTo(toEmail);
            helper.setSubject("Mã OTP đặt lại mật khẩu - Movie Project");

            // Đọc HTML template
            String htmlContent = loadEmailTemplate("templates/otp-email.html");
            // Thay thế placeholder
            htmlContent = htmlContent.replace("${otp}", otp);

            helper.setText(htmlContent, true); // true = HTML

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

    /**
     * Load email template từ resources (với cache)
     */
    private String loadEmailTemplate(String templatePath) throws IOException {
        // Lấy từ cache nếu có
        String cached = templateCache.get(templatePath);
        if (cached != null) {
            return cached;
        }

        // Đọc file và cache
        try {
            ClassPathResource resource = new ClassPathResource(templatePath);
            String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            templateCache.put(templatePath, content);
            return content;
        } catch (IOException e) {
            log.error("❌ Lỗi khi đọc email template: {}", templatePath, e);
            throw e;
        }
    }
}

