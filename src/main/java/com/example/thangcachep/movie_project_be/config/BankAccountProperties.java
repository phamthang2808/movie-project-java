package com.example.thangcachep.movie_project_be.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties cho thông tin tài khoản ngân hàng
 * Đọc từ application.yml với prefix: app.bank-account
 */
@Configuration
@ConfigurationProperties(prefix = "app.bank-account")
@Data
public class BankAccountProperties {
    /**
     * Tên ngân hàng (ví dụ: BIDV, VCB, TCB, ...)
     */
    private String bankName = "BIDV";

    /**
     * Số tài khoản ngân hàng
     */
    private String accountNumber;

    /**
     * Tên chủ tài khoản
     */
    private String accountName;
}

