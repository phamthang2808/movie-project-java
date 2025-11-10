package com.example.thangcachep.movie_project_be.services.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import com.example.thangcachep.movie_project_be.config.BankAccountProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.thangcachep.movie_project_be.entities.TransactionEntity;
import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.repositories.TransactionRepository;
import com.example.thangcachep.movie_project_be.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý chuyển khoản ngân hàng
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BankTransferService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final QrCodeService qrCodeService;
    private final BankAccountProperties bankAccountProperties;

    /**
     * Tạo mã giao dịch chuyển khoản ngân hàng
     * Mã này user sẽ ghi vào nội dung chuyển khoản
     *
     * @param userId ID của user
     * @param amount Số tiền cần nạp
     * @return Transaction code (mã giao dịch) và thông tin tài khoản
     */
    @Transactional
    public Map<String, Object> createBankTransferTransaction(Long userId, Double amount) {
        log.info("📝 Tạo giao dịch chuyển khoản - UserId: {}, Amount: {} VND", userId, amount);

        // Lấy user
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        // Tạo mã giao dịch unique (8 ký tự: chữ + số)
        String transactionCode = generateTransactionCode();

        // Kiểm tra mã đã tồn tại chưa (rất hiếm nhưng vẫn check)
        while (transactionRepository.findByPaymentId(transactionCode).isPresent()) {
            transactionCode = generateTransactionCode();
        }

        // Tính thời gian hết hạn (30 phút)
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(30);

        // Tạo transaction với status PENDING
        TransactionEntity transaction = TransactionEntity.builder()
                .user(user)
                .type(TransactionEntity.TransactionType.RECHARGE)
                .status(TransactionEntity.TransactionStatus.PENDING)
                .amount(amount)
                .description("Nạp tiền qua chuyển khoản ngân hàng - Mã: " + transactionCode)
                .paymentMethod("BANK_TRANSFER")
                .paymentId(transactionCode) // Lưu mã giao dịch vào paymentId
                .build();

        transaction = transactionRepository.save(transaction);

        log.info("✅ Đã tạo giao dịch chuyển khoản - TransactionId: {}, Code: {}",
                transaction.getId(), transactionCode);

        // Thông tin tài khoản ngân hàng (có thể lấy từ config hoặc database)
        Map<String, String> bankInfo = getBankAccountInfo();

        // Tạo VietQR code với thông tin đầy đủ
        String qrCodeData = generateVietQRCode(
                bankInfo.get("accountNumber"),
                bankInfo.get("accountName"),
                bankInfo.get("bankName"),
                amount,
                transactionCode
        );

        // Generate QR code image
        String qrCodeDataUrl = qrCodeService.generateQRCodeDataUrl(qrCodeData, 400, 400);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("transactionId", transaction.getId());
        response.put("transactionCode", transactionCode);
        response.put("amount", amount);
        response.put("expireTime", expireTime);
        response.put("bankInfo", bankInfo);
        response.put("qrCodeDataUrl", qrCodeDataUrl);
        response.put("qrCodeData", qrCodeData); // QR code text để debug
        response.put("message", "Vui lòng chuyển khoản với nội dung: " + transactionCode);

        log.info("✅ Đã tạo VietQR code cho giao dịch - Code: {}", transactionCode);

        return response;
    }

    /**
     * Verify và xử lý chuyển khoản ngân hàng
     * Có thể gọi từ:
     * - Admin verify manual
     * - API ngân hàng callback (nếu tích hợp)
     * - Scheduled job check (nếu có API ngân hàng)
     *
     * @param transactionCode Mã giao dịch
     * @param verifiedBy Ai verify (ADMIN, SYSTEM, BANK_API)
     * @return Kết quả verify
     */
    @Transactional
    public Map<String, Object> verifyBankTransfer(String transactionCode, String verifiedBy) {
        log.info("🔍 Verify chuyển khoản - Code: {}, VerifiedBy: {}", transactionCode, verifiedBy);

        // Tìm transaction theo paymentId (transactionCode)
        Optional<TransactionEntity> transactionOpt = transactionRepository.findByPaymentId(transactionCode);

        if (transactionOpt.isEmpty()) {
            log.warn("❌ Không tìm thấy giao dịch với mã: {}", transactionCode);
            return Map.of(
                    "success", false,
                    "message", "Không tìm thấy giao dịch với mã: " + transactionCode
            );
        }

        TransactionEntity transaction = transactionOpt.get();

        // Kiểm tra transaction đã được xử lý chưa
        if (transaction.getStatus() == TransactionEntity.TransactionStatus.COMPLETED) {
            log.warn("⚠️ Giao dịch đã được xử lý trước đó - Code: {}", transactionCode);
            return Map.of(
                    "success", true,
                    "message", "Giao dịch đã được xử lý trước đó",
                    "alreadyProcessed", true
            );
        }

        // Kiểm tra transaction đã hết hạn chưa (30 phút)
        LocalDateTime expireTime = transaction.getCreatedAt().plusMinutes(30);
        if (LocalDateTime.now().isAfter(expireTime)) {
            log.warn("⏰ Giao dịch đã hết hạn - Code: {}", transactionCode);
            transaction.setStatus(TransactionEntity.TransactionStatus.CANCELLED);
            transactionRepository.save(transaction);
            return Map.of(
                    "success", false,
                    "message", "Giao dịch đã hết hạn"
            );
        }

        // Verify thành công - Cộng tiền vào balance
        UserEntity user = transaction.getUser();
        double currentBalance = user.getBalance() != null ? user.getBalance() : 0.0;
        double newBalance = currentBalance + transaction.getAmount();
        user.setBalance(newBalance);
        userRepository.save(user);

        // Cập nhật transaction status
        transaction.setStatus(TransactionEntity.TransactionStatus.COMPLETED);
        transaction.setCompletedAt(LocalDateTime.now());
        transaction.setDescription(transaction.getDescription() + " - Verified by: " + verifiedBy);
        transactionRepository.save(transaction);

        log.info("✅ Đã verify và cộng {} VND vào số dư user {} (số dư cũ: {} VND, số dư mới: {} VND)",
                transaction.getAmount(), user.getId(), currentBalance, newBalance);

        return Map.of(
                "success", true,
                "message", "Xác nhận chuyển khoản thành công",
                "amount", transaction.getAmount(),
                "newBalance", newBalance
        );
    }

    /**
     * Generate mã giao dịch unique (8 ký tự: chữ + số)
     */
    private String generateTransactionCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder(8);

        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }

        return code.toString();
    }

    /**
     * Lấy thông tin tài khoản ngân hàng từ configuration
     */
    private Map<String, String> getBankAccountInfo() {
        Map<String, String> bankInfo = new HashMap<>();
        bankInfo.put("bankName", bankAccountProperties.getBankName() != null
                ? bankAccountProperties.getBankName() : "BIDV");
        bankInfo.put("accountNumber", bankAccountProperties.getAccountNumber() != null
                ? bankAccountProperties.getAccountNumber() : "");
        bankInfo.put("accountName", bankAccountProperties.getAccountName() != null
                ? bankAccountProperties.getAccountName() : "");

        // Validate thông tin
        if (bankInfo.get("accountNumber").isEmpty() || bankInfo.get("accountName").isEmpty()) {
            log.warn("⚠️ Thông tin tài khoản ngân hàng chưa được cấu hình đầy đủ trong application.yml");
        }

        return bankInfo;
    }

    /**
     * Tạo VietQR code với format EMV QR Code
     * Format: https://www.vietqr.io/ hoặc EMV QR Code standard
     *
     * @param accountNumber Số tài khoản
     * @param accountName Tên chủ tài khoản
     * @param bankName Tên ngân hàng
     * @param amount Số tiền
     * @param content Nội dung chuyển khoản
     * @return QR code data string
     */
    private String generateVietQRCode(String accountNumber, String accountName,
                                      String bankName, Double amount, String content) {
        // Format 1: Sử dụng VietQR.io API format (đơn giản, dễ đọc)
        // Format: https://vietqr.io/{bankCode}/{accountNumber}?amount={amount}&addInfo={content}

        // Map bank name to bank code (VietQR format)
        String bankCode = mapBankNameToCode(bankName);

        // Tạo URL VietQR
        StringBuilder qrData = new StringBuilder();
        qrData.append("https://vietqr.io/");
        qrData.append(bankCode);
        qrData.append("/");
        qrData.append(accountNumber);
        qrData.append("?amount=");
        qrData.append(amount.longValue()); // Số tiền (không có dấu phẩy)
        qrData.append("&addInfo=");
        qrData.append(java.net.URLEncoder.encode(content, java.nio.charset.StandardCharsets.UTF_8));
        qrData.append("&accountName=");
        qrData.append(java.net.URLEncoder.encode(accountName, java.nio.charset.StandardCharsets.UTF_8));

        String qrString = qrData.toString();
        log.info("📱 Đã tạo VietQR code: {}", qrString);

        return qrString;
    }

    /**
     * Map tên ngân hàng sang mã ngân hàng (VietQR format)
     */
    private String mapBankNameToCode(String bankName) {
        if (bankName == null) {
            return "BIDV"; // Default
        }

        String bankUpper = bankName.toUpperCase();

        // Map các ngân hàng phổ biến
        return switch (bankUpper) {
            case "BIDV", "NGAN HANG BIDV" -> "BIDV";
            case "VCB", "VIETCOMBANK", "NGAN HANG VIETCOMBANK" -> "VCB";
            case "TCB", "TECHCOMBANK", "NGAN HANG TECHCOMBANK" -> "TCB";
            case "VTB", "VIETINBANK", "NGAN HANG VIETINBANK" -> "VTB";
            case "ACB", "NGAN HANG ACB" -> "ACB";
            case "TPB", "TPBANK", "NGAN HANG TPBANK" -> "TPB";
            case "VPB", "VPBANK", "NGAN HANG VPBANK" -> "VPB";
            case "MSB", "NGAN HANG MSB" -> "MSB";
            case "HDB", "HDBANK", "NGAN HANG HDBANK" -> "HDB";
            case "VIB", "NGAN HANG VIB" -> "VIB";
            case "SHB", "NGAN HANG SHB" -> "SHB";
            case "OCB", "NGAN HANG OCB" -> "OCB";
            case "MBB", "MBBANK", "NGAN HANG MBBANK" -> "MBB";
            case "VCCB", "NGAN HANG VCCB" -> "VCCB";
            case "AGB", "AGRIBANK", "NGAN HANG AGRIBANK" -> "AGB";
            default -> "BIDV"; // Default fallback
        };
    }
}

