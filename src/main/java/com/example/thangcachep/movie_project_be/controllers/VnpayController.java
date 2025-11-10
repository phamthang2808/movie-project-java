package com.example.thangcachep.movie_project_be.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.models.request.VnpayRequest;
import com.example.thangcachep.movie_project_be.models.responses.VnpayPaymentResponse;
import com.example.thangcachep.movie_project_be.services.impl.VnpayService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("${api.prefix}/vnpay")
@RequiredArgsConstructor
@Slf4j
public class VnpayController {

    private final VnpayService vnpayService;

    /**
     * Tạo payment URL cho VNPay (backward compatible)
     * POST /api/v1/vnpay/create
     */
    @PostMapping("/create")
    public ResponseEntity<String> createPayment(@RequestBody VnpayRequest paymentRequest) {
        try {
            log.info("📨 Nhận request tạo VNPay payment - Số tiền: {} VND", paymentRequest.getAmount());

            // Lấy userId từ SecurityContext để lưu vào OrderInfo
            Long userId = null;
            try {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getPrincipal() instanceof UserEntity) {
                    UserEntity user = (UserEntity) authentication.getPrincipal();
                    userId = user.getId();
                    log.info("VNPay create: Lấy userId từ SecurityContext: {}", userId);
                }
            } catch (Exception e) {
                log.warn("VNPay create: Không thể lấy userId từ SecurityContext: {}", e.getMessage());
            }

            String paymentUrl = vnpayService.createPayment(paymentRequest, userId);

            log.info("✅ Trả về payment URL cho client");
            return ResponseEntity.ok(paymentUrl);

        } catch (IllegalArgumentException e) {
            log.error("❌ Lỗi validate: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("❌ Lỗi tạo VNPay payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Đã xảy ra lỗi khi tạo thanh toán!");
        }
    }

    /**
     * Tạo payment URL và QR code cho VNPay
     * POST /api/v1/vnpay/create-qr
     */
    @PostMapping("/create-qr")
    public ResponseEntity<?> createPaymentWithQR(@RequestBody VnpayRequest paymentRequest) {
        try {
            log.info("📨 Nhận request tạo VNPay payment với QR code - Số tiền: {} VND", paymentRequest.getAmount());

            // Lấy userId từ SecurityContext để lưu vào OrderInfo
            Long userId = null;
            try {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getPrincipal() instanceof UserEntity) {
                    UserEntity user = (UserEntity) authentication.getPrincipal();
                    userId = user.getId();
                    log.info("VNPay create-qr: Lấy userId từ SecurityContext: {}", userId);
                }
            } catch (Exception e) {
                log.warn("VNPay create-qr: Không thể lấy userId từ SecurityContext: {}", e.getMessage());
            }

            VnpayPaymentResponse response = vnpayService.createPaymentWithQR(paymentRequest, userId);

            log.info("✅ Trả về VNPay payment với QR code cho client - TxnRef: {}", response.getTransactionRef());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("❌ Lỗi validate: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Lỗi tạo VNPay payment với QR code: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "❌ Đã xảy ra lỗi khi tạo thanh toán!"));
        }
    }

    /**
     * Callback URL từ VNPay sau khi thanh toán
     * GET /api/v1/vnpay/return?vnp_ResponseCode=00
     */
    @GetMapping("/return")
    public ResponseEntity<?> vnpReturn(@RequestParam Map<String, String> params) {
        // Lấy userId từ SecurityContext (nếu user đã đăng nhập)
        // Nếu không có, sẽ parse từ OrderInfo trong verifyAndProcess()
        Long userId = null;
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserEntity) {
                UserEntity user = (UserEntity) authentication.getPrincipal();
                userId = user.getId();
                log.info("VNPay return: Lấy userId từ SecurityContext: {}", userId);
            }
        } catch (Exception e) {
            log.debug("VNPay return: Không thể lấy userId từ SecurityContext (sẽ parse từ OrderInfo): {}", e.getMessage());
        }

        return vnpayService.verifyAndProcess(params, false, userId);
    }

    // IPN URL (server-to-server, dùng để chốt giao dịch, độ tin cậy cao)
    @GetMapping("/ipn")
    public ResponseEntity<String> vnpIpn(@RequestParam Map<String, String> params) {
        // IPN không có authentication, cần parse userId từ OrderInfo
        // Ví dụ: "USER_123_ORDER_71082970" -> userId = 123
        Long userId = null;
        try {
            String orderInfo = params.get("vnp_OrderInfo");
            if (orderInfo != null && orderInfo.contains("USER_")) {
                String[] parts = orderInfo.split("_");
                if (parts.length >= 2) {
                    userId = Long.parseLong(parts[1]);
                    log.info("VNPay IPN: Parse userId từ OrderInfo: {}", userId);
                }
            }
        } catch (Exception e) {
            log.warn("VNPay IPN: Không thể parse userId từ OrderInfo: {}", e.getMessage());
        }

        // Theo spec VNPay, IPN nên trả về chuỗi (OK/ERROR...) – tuỳ yêu cầu bạn có thể thay đổi
        return vnpayService.verifyAndProcess(params, true, userId).getStatusCode().is2xxSuccessful()
                ? ResponseEntity.ok("OK")
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ERROR");
    }
}