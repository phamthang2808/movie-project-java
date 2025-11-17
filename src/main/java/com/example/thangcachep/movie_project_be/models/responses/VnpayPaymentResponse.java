package com.example.thangcachep.movie_project_be.models.responses;


import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VnpayPaymentResponse {

    /**
     * Payment URL để redirect user đến trang VNPay
     */
    private String paymentUrl;

    /**
     * QR Code image dạng base64 (có thể dùng trực tiếp trong <img src="data:image/png;base64,...">)
     */
    private String qrCodeBase64;

    /**
     * QR Code data URL (đầy đủ format: "data:image/png;base64,...")
     */
    private String qrCodeDataUrl;

    /**
     * Mã giao dịch (Transaction Reference)
     */
    private String transactionRef;

    /**
     * Số tiền thanh toán (VND)
     */
    private Long amount;

    /**
     * Thời gian hết hạn (milliseconds)
     */
    private Long expireTime;
}

