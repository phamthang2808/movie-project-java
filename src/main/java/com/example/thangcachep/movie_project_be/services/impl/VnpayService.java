package com.example.thangcachep.movie_project_be.services.impl;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.example.thangcachep.movie_project_be.config.VnPayConfig;
import com.example.thangcachep.movie_project_be.models.request.VnpayRequest;

import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class VnpayService {

    public String createPayment(VnpayRequest paymentRequest) throws UnsupportedEncodingException {
        log.info("📝 Bắt đầu tạo payment VNPay - Số tiền: {} VND", paymentRequest.getAmount());
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String orderType = "other";

        long amount = 0;
        try {
            amount = Long.parseLong(paymentRequest.getAmount()) * 100;
            log.debug("💰 Số tiền sau khi convert: {} (x100)", amount);
        } catch (NumberFormatException e) {
            log.error("❌ Số tiền không hợp lệ: {}", paymentRequest.getAmount());
            throw new IllegalArgumentException("Số tiền không hợp lệ");
        }

        String bankCode = "NCB";
        String vnp_TxnRef = VnPayConfig.getRandomNumber(8);
        String vnp_IpAddr = "127.0.0.1";
        String vnp_TmnCode = VnPayConfig.vnp_TmnCode;

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");

        vnp_Params.put("vnp_BankCode", bankCode);
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang:" + vnp_TxnRef);
        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", VnPayConfig.vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName).append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()))
                        .append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                query.append('&');
                hashData.append('&');
            }
        }

        if (query.length() > 0)
            query.setLength(query.length() - 1);
        if (hashData.length() > 0)
            hashData.setLength(hashData.length() - 1);

        String vnp_SecureHash = VnPayConfig.hmacSHA512(VnPayConfig.vnp_SecretKey, hashData.toString());
        query.append("&vnp_SecureHash=").append(vnp_SecureHash);
        
        String paymentUrl = VnPayConfig.vnp_PayUrl + "?" + query;
        log.info("✅ Tạo VNPay payment URL thành công - Mã giao dịch: {}", vnp_TxnRef);
        log.debug("🔗 Payment URL: {}", paymentUrl);
        
        return paymentUrl;
    }

    public ResponseEntity<String> handlePaymentReturn(String responseCode) {
        log.info("🔙 Nhận callback từ VNPay - Response Code: {}", responseCode);
        
        if ("00".equals(responseCode)) {
            log.info("✅ Thanh toán VNPay thành công!");
            return ResponseEntity.ok("✅ Thanh toán thành công!");
        } else {
            log.warn("❌ Thanh toán VNPay thất bại - Mã lỗi: {}", responseCode);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("❌ Thanh toán thất bại! Mã lỗi: " + responseCode);
        }
    }
}