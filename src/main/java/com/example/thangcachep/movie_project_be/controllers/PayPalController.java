package com.example.thangcachep.movie_project_be.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.thangcachep.movie_project_be.components.LocalizationUtils;
import com.example.thangcachep.movie_project_be.exceptions.InvalidParamException;
import com.example.thangcachep.movie_project_be.models.request.PayPalPaymentRequest;
import com.example.thangcachep.movie_project_be.models.responses.ApiResponse;
import com.example.thangcachep.movie_project_be.services.impl.PayPalService;
import com.example.thangcachep.movie_project_be.utils.MessageKeys;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("${api.prefix}/paypal")
@RequiredArgsConstructor
@Slf4j
public class PayPalController {

    private final PayPalService payPalService;
    private final LocalizationUtils localizationUtils;

    /**
     * Tạo payment và nhận approval URL
     * POST /api/v1/paypal/create
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPayment(@RequestBody PayPalPaymentRequest request) {
        log.info("📨 Nhận request tạo PayPal payment - Số tiền: {} {}",
                request.getAmount(), request.getCurrency());

        // Validate request
        if (request.getAmount() == null || request.getAmount() <= 0) {
            log.warn("⚠️ Số tiền không hợp lệ: {}", request.getAmount());
            throw new InvalidParamException("Số tiền không hợp lệ");
        }

        if (request.getCurrency() == null || request.getCurrency().isEmpty()) {
            request.setCurrency("USD"); // Default currency
            log.info("💱 Sử dụng tiền tệ mặc định: USD");
        }

        // Create payment
        String approvalUrl = payPalService.createPayment(request);

        Map<String, Object> data = new HashMap<>();
        data.put("approvalUrl", approvalUrl);

        log.info("✅ Trả về approval URL cho client");
        String message = localizationUtils.getLocalizedMessage(MessageKeys.PAYMENT_PAYPAL_CREATE_SUCCESS);
        ApiResponse<Map<String, Object>> response = ApiResponse.success(message, data);
        return ResponseEntity.ok(response);
    }

    /**
     * Callback sau khi user approve payment trên PayPal
     * GET /api/v1/paypal/success?token=ORDER_ID
     */
    @GetMapping("/success")
    public ResponseEntity<ApiResponse<Map<String, Object>>> successPayment(@RequestParam("token") String orderId) {
        log.info("🎉 User đã approve payment - Order ID: {}", orderId);

        String result = payPalService.capturePayment(orderId);

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("message", result);

        log.info("✅ Capture payment thành công");
        String message = localizationUtils.getLocalizedMessage(MessageKeys.PAYMENT_PAYPAL_SUCCESS);
        ApiResponse<Map<String, Object>> response = ApiResponse.success(message, data);
        return ResponseEntity.ok(response);
    }

    /**
     * Callback khi user cancel payment
     * GET /api/v1/paypal/cancel
     */
    @GetMapping("/cancel")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelPayment() {
        log.warn("❌ User đã hủy thanh toán PayPal");

        Map<String, Object> data = new HashMap<>();
        data.put("cancelled", true);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.PAYMENT_PAYPAL_CANCELLED);
        ApiResponse<Map<String, Object>> response = ApiResponse.error(message, 200);
        return ResponseEntity.ok(response);
    }
}