package com.example.thangcachep.movie_project_be.services.impl;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.thangcachep.movie_project_be.config.VnPayConfig;
import com.example.thangcachep.movie_project_be.entities.TransactionEntity;
import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.exceptions.InvalidParamException;
import com.example.thangcachep.movie_project_be.models.request.VnpayRequest;
import com.example.thangcachep.movie_project_be.models.responses.VnpayPaymentResponse;
import com.example.thangcachep.movie_project_be.repositories.TransactionRepository;
import com.example.thangcachep.movie_project_be.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
@RequiredArgsConstructor
public class VnpayService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final QrCodeService qrCodeService;


    public String createPayment(VnpayRequest paymentRequest, Long userId) throws UnsupportedEncodingException {
        log.info("📝 Bắt đầu tạo payment VNPay - Số tiền: {} VND, UserId: {}", paymentRequest.getAmount(), userId);
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String orderType = "other";

        long amount = 0;
        try {
            amount = Long.parseLong(paymentRequest.getAmount()) * 100;
            log.debug("💰 Số tiền sau khi convert: {} (x100)", amount);
        } catch (NumberFormatException e) {
            log.error("❌ Số tiền không hợp lệ: {}", paymentRequest.getAmount());
            throw new InvalidParamException("Số tiền không hợp lệ");
        }

        // Sử dụng bankCode từ request, mặc định là BIDV nếu không có
        String bankCode = (paymentRequest.getBankCode() != null && !paymentRequest.getBankCode().isEmpty())
                ? paymentRequest.getBankCode()
                : "BIDV";

        log.info("🏦 Ngân hàng được chọn: {}", bankCode);

        String vnp_TxnRef = VnPayConfig.getRandomNumber(8);
        String vnp_IpAddr = "127.0.0.1";
        String vnp_TmnCode = VnPayConfig.vnp_TmnCode;

        // Encode userId vào OrderInfo để có thể lấy lại khi verify
        String orderInfo = "Thanh toan don hang:" + vnp_TxnRef;
        if (userId != null) {
            orderInfo = "USER_" + userId + "_ORDER_" + vnp_TxnRef;
            log.info("🔐 Đã encode userId {} vào OrderInfo", userId);
        }

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");

        vnp_Params.put("vnp_BankCode", bankCode);
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", orderInfo);
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

    /**
     * Tạo payment URL và QR code cho VNPay
     *
     * @param paymentRequest Thông tin thanh toán
     * @param userId ID của user
     * @return VnpayPaymentResponse chứa payment URL và QR code
     * @throws UnsupportedEncodingException
     */
    public VnpayPaymentResponse createPaymentWithQR(VnpayRequest paymentRequest, Long userId)
            throws UnsupportedEncodingException {
        log.info("📝 Bắt đầu tạo VNPay payment với QR code - Số tiền: {} VND, UserId: {}",
                paymentRequest.getAmount(), userId);

        // Tạo payment URL như bình thường
        String paymentUrl = createPayment(paymentRequest, userId);

        // Extract transaction reference từ URL
        String vnp_TxnRef = null;
        try {
            String[] urlParts = paymentUrl.split("vnp_TxnRef=");
            if (urlParts.length > 1) {
                String[] refParts = urlParts[1].split("&");
                vnp_TxnRef = refParts[0];
            }
        } catch (Exception e) {
            log.warn("Không thể extract transaction ref từ URL: {}", e.getMessage());
        }

        // Generate QR code từ payment URL
        String qrCodeBase64 = qrCodeService.generateQRCodeBase64(paymentUrl, 300, 300);
        String qrCodeDataUrl = qrCodeService.generateQRCodeDataUrl(paymentUrl, 300, 300);

        // Parse amount
        long amount = Long.parseLong(paymentRequest.getAmount());

        // Calculate expire time (15 minutes from now)
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        cld.add(Calendar.MINUTE, 15);
        long expireTime = cld.getTimeInMillis();

        log.info("✅ Đã tạo VNPay payment với QR code - TxnRef: {}", vnp_TxnRef);

        return VnpayPaymentResponse.builder()
                .paymentUrl(paymentUrl)
                .qrCodeBase64(qrCodeBase64)
                .qrCodeDataUrl(qrCodeDataUrl)
                .transactionRef(vnp_TxnRef)
                .amount(amount)
                .expireTime(expireTime)
                .build();
    }

    @Transactional
    @CacheEvict(value = "statistics", allEntries = true)
    public ResponseEntity<?> verifyAndProcess(Map<String, String> params, boolean isIpn, Long userId) {
        try {
            // 1) Lấy secure hash và tạo bản sao fields để tính lại hash
            String secureHash = params.get("vnp_SecureHash");
            Map<String, String> fields = new HashMap<>(params);
            fields.remove("vnp_SecureHash");
            fields.remove("vnp_SecureHashType");

            // 2) Sort key + build hashData (encoding US_ASCII như lúc tạo đơn)
            List<String> fieldNames = new ArrayList<>(fields.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            for (String name : fieldNames) {
                String value = fields.get(name);
                if (value != null && !value.isEmpty()) {
                    hashData.append(name).append('=')
                            .append(URLEncoder.encode(value, StandardCharsets.US_ASCII));
                    hashData.append('&');
                }
            }
            if (hashData.length() > 0) hashData.setLength(hashData.length() - 1);

            // 3) Tự tính lại hash và so sánh
            String calcHash = VnPayConfig.hmacSHA512(VnPayConfig.vnp_SecretKey, hashData.toString());
            if (!calcHash.equalsIgnoreCase(secureHash)) {
                log.warn("VNPay: chữ ký không hợp lệ");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("INVALID_SIGNATURE");
            }

            // 4) Check trạng thái
            boolean ok = "00".equals(params.get("vnp_ResponseCode"))
                    && "00".equals(params.get("vnp_TransactionStatus"));
            if (!ok) {
                log.warn("VNPay: thanh toán thất bại - code: {}", params.get("vnp_ResponseCode"));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("PAYMENT_FAILED:" + params.get("vnp_ResponseCode"));
            }

            // 5) Parse thông tin giao dịch
            String txnRef = params.get("vnp_TxnRef");
            String txnNo = params.get("vnp_TransactionNo");
            long amountVnd = Long.parseLong(params.getOrDefault("vnp_Amount", "0")) / 100;

            // 5.5) IDEMPOTENT CHECK - Kiểm tra trùng giao dịch để tránh double payment
            Optional<TransactionEntity> existingTransaction = transactionRepository.findByPaymentId(txnNo);
            if (existingTransaction.isPresent()) {
                TransactionEntity existing = existingTransaction.get();
                log.warn("VNPay: Giao dịch đã được xử lý trước đó - TxnNo: {}, Status: {}, Amount: {} VND",
                        txnNo, existing.getStatus(), existing.getAmount());

                // Nếu transaction đã completed, return success nhưng không cộng tiền nữa
                if (existing.getStatus() == TransactionEntity.TransactionStatus.COMPLETED) {
                    log.info("VNPay: Transaction {} đã completed, bỏ qua xử lý (idempotent)", txnNo);
                    Map<String, Object> response = new HashMap<>();
                    response.put("message", "Giao dịch đã được xử lý trước đó");
                    response.put("success", true);
                    response.put("amount", existing.getAmount());
                    response.put("transactionNo", txnNo);
                    response.put("alreadyProcessed", true);
                    return ResponseEntity.ok(response);
                }
            }

            // 6) Lấy userId từ OrderInfo nếu chưa có từ SecurityContext
            if (userId == null) {
                String orderInfo = params.get("vnp_OrderInfo");
                if (orderInfo != null && orderInfo.contains("USER_")) {
                    try {
                        String[] parts = orderInfo.split("_");
                        if (parts.length >= 2) {
                            userId = Long.parseLong(parts[1]);
                            log.info("VNPay: Parse userId từ OrderInfo: {}", userId);
                        }
                    } catch (NumberFormatException e) {
                        log.warn("VNPay: Không thể parse userId từ OrderInfo: {}", orderInfo);
                    }
                }
            }

            if (userId == null) {
                log.error("VNPay: Không thể xác định userId từ request hoặc OrderInfo");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("CANNOT_IDENTIFY_USER");
            }

            // 7) Lấy user và cập nhật số dư
            Optional<UserEntity> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                log.error("VNPay: Không tìm thấy user với ID: {}", userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("USER_NOT_FOUND");
            }

            UserEntity user = userOpt.get();
            double currentBalance = user.getBalance() != null ? user.getBalance() : 0.0;
            double newBalance = currentBalance + amountVnd;
            user.setBalance(newBalance);
            userRepository.save(user);

            log.info("VNPay: Đã cộng {} VND vào số dư user {} (số dư cũ: {} VND, số dư mới: {} VND)",
                    amountVnd, userId, currentBalance, newBalance);

            // 8) Lưu transaction vào database để tránh trùng giao dịch sau này
            try {
                TransactionEntity transaction = TransactionEntity.builder()
                        .user(user)
                        .type(TransactionEntity.TransactionType.RECHARGE)
                        .status(TransactionEntity.TransactionStatus.COMPLETED)
                        .amount((double) amountVnd)
                        .description("Nạp tiền qua VNPay - Mã GD: " + txnRef)
                        .paymentMethod("VNPAY")
                        .paymentId(txnNo) // Lưu vnp_TransactionNo để check trùng
                        .completedAt(LocalDateTime.now())
                        .build();
                transactionRepository.save(transaction);
                log.info("VNPay: Đã lưu transaction vào database - TxnNo: {}, TxnRef: {}", txnNo, txnRef);
            } catch (Exception e) {
                // Nếu có unique constraint violation (race condition), kiểm tra lại
                Optional<TransactionEntity> duplicateCheck = transactionRepository.findByPaymentId(txnNo);
                if (duplicateCheck.isPresent()) {
                    log.warn("VNPay: Transaction {} đã được xử lý bởi request khác (race condition), rollback balance", txnNo);
                    // Rollback balance nếu transaction đã tồn tại
                    user.setBalance(currentBalance);
                    userRepository.save(user);
                    log.info("VNPay: Đã rollback balance cho user {} về {}", userId, currentBalance);
                }
                // Nếu là lỗi khác, vẫn log nhưng không rollback (vì có thể đã commit)
                log.error("VNPay: Lỗi khi lưu transaction - TxnNo: {}, Error: {}", txnNo, e.getMessage());
            }
            log.info("VNPay: xử lý thành công TxnRef={}, TxnNo={}, amount={} VND, userId={}, ipn={}",
                    txnRef, txnNo, amountVnd, userId, isIpn);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Thanh toán thành công!");
            response.put("success", true);
            response.put("amount", amountVnd);
            response.put("transactionNo", txnNo);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("VNPay verify error: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("SERVER_ERROR");
        }
    }
}
