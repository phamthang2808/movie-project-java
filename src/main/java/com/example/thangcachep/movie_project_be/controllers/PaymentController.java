package com.example.thangcachep.movie_project_be.controllers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.thangcachep.movie_project_be.components.LocalizationUtils;
import com.example.thangcachep.movie_project_be.entities.TransactionEntity;
import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.exceptions.InvalidParamException;
import com.example.thangcachep.movie_project_be.models.responses.ApiResponse;
import com.example.thangcachep.movie_project_be.models.responses.UserResponse;
import com.example.thangcachep.movie_project_be.repositories.TransactionRepository;
import com.example.thangcachep.movie_project_be.repositories.UserRepository;
import com.example.thangcachep.movie_project_be.services.impl.BankTransferService;
import com.example.thangcachep.movie_project_be.services.impl.UserService;
import com.example.thangcachep.movie_project_be.utils.MessageKeys;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final TransactionRepository transactionRepository;
    private final BankTransferService bankTransferService;
    private final LocalizationUtils localizationUtils;

    /**
     * Mua VIP - Check balance, trừ tiền, update VIP expiration
     */
    @PostMapping("/upgrade-vip")
    public ResponseEntity<ApiResponse<Map<String, Object>>> upgradeToVip(@RequestBody Map<String, Object> request) {
        UserEntity user = getCurrentUser();

        // Lấy thông tin từ request
        Integer planId = (Integer) request.get("planId");
        Integer price = (Integer) request.get("price");

        if (planId == null || price == null) {
            throw new InvalidParamException("Thiếu thông tin gói VIP");
        }

        // Kiểm tra số dư
        if (user.getBalance() < price) {
            throw new InvalidParamException("Số dư không đủ! Vui lòng nạp thêm tiền.");
        }

            // Tính thời hạn VIP
            LocalDateTime currentExpiration = user.getVipExpiredAt();
            LocalDateTime now = LocalDateTime.now();

            // Nếu đã có VIP và chưa hết hạn, cộng thêm vào thời hạn hiện tại
            // Nếu chưa có VIP hoặc đã hết hạn, tính từ bây giờ
            LocalDateTime newExpiration;
            if (currentExpiration != null && currentExpiration.isAfter(now)) {
                newExpiration = currentExpiration;
            } else {
                newExpiration = now;
            }

            // Thêm thời hạn theo gói
            int months = getMonthsByPlanId(planId);
            newExpiration = newExpiration.plusMonths(months);

            // Trừ tiền
            double newBalance = user.getBalance() - price;
            user.setBalance(newBalance);

            // Cập nhật VIP status
            user.setIsVip(true);
            user.setVipExpiredAt(newExpiration);

            // Lưu user
            userRepository.save(user);

            // Tạo transaction record
            TransactionEntity transaction = TransactionEntity.builder()
                    .user(user)
                    .type(TransactionEntity.TransactionType.VIP_PURCHASE)
                    .amount((double) price)
                    .status(TransactionEntity.TransactionStatus.COMPLETED)
                    .completedAt(LocalDateTime.now())
                    .description("Mua VIP gói " + getMonthsByPlanId(planId) + " tháng")
                    .build();
            transactionRepository.save(transaction);

            // Map to UserResponse để trả về
            UserResponse userResponse = userService.mapToUserResponse(user);

            Map<String, Object> data = new HashMap<>();
            data.put("user", userResponse);
            data.put("transaction", Map.of(
                    "id", transaction.getId(),
                    "amount", transaction.getAmount(),
                    "type", transaction.getType().name()
            ));

            String message = localizationUtils.getLocalizedMessage(MessageKeys.PAYMENT_VIP_UPGRADE_SUCCESS);
            ApiResponse<Map<String, Object>> response = ApiResponse.success(message, data);
            return ResponseEntity.ok(response);
    }

    /**
     * Lấy số tháng theo planId
     */
    private int getMonthsByPlanId(Integer planId) {
        return switch (planId) {
            case 1 -> 1;  // 1 tháng
            case 2 -> 6;  // 6 tháng
            case 3 -> 12; // 12 tháng
            default -> 1;
        };
    }

    private UserEntity getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (UserEntity) authentication.getPrincipal();
    }

    /**
     * Lấy lịch sử giao dịch của user
     * CHỈ trả về các giao dịch RECHARGE (nạp tiền VNPay), BỎ VIP_PURCHASE
     */
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTransactionHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UserEntity user = getCurrentUser();

            // Lấy tất cả transactions của user
            List<TransactionEntity> allTransactions =
                    transactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

            // Lọc CHỈ lấy RECHARGE (nạp tiền VNPay), BỎ VIP_PURCHASE
            List<TransactionEntity> rechargeTransactions = allTransactions.stream()
                    .filter(t -> t.getType() == TransactionEntity.TransactionType.RECHARGE)
                    .collect(Collectors.toList());

            // Phân trang
            int total = rechargeTransactions.size();
            int start = page * size;
            int end = Math.min(start + size, total);
            List<TransactionEntity> pagedTransactions =
                    start < total ? rechargeTransactions.subList(start, end) : List.of();

            // Map to response format
            List<Map<String, Object>> transactionList = pagedTransactions.stream()
                    .map(t -> {
                        Map<String, Object> transactionMap = new HashMap<>();
                        transactionMap.put("id", t.getId());
                        transactionMap.put("amount", t.getAmount());
                        transactionMap.put("type", t.getType().name());
                        transactionMap.put("status", t.getStatus().name());
                        transactionMap.put("description", t.getDescription());
                        transactionMap.put("paymentMethod", t.getPaymentMethod());
                        transactionMap.put("paymentId", t.getPaymentId());
                        transactionMap.put("completedAt", t.getCompletedAt());
                        transactionMap.put("createdAt", t.getCreatedAt());
                        return transactionMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("transactions", transactionList);
            data.put("total", total);
            data.put("page", page);
            data.put("size", size);
            data.put("totalPages", (int) Math.ceil((double) total / size));

            String message = localizationUtils.getLocalizedMessage(MessageKeys.STATISTICS_GET_SUCCESS); // Hoặc tạo key mới
            ApiResponse<Map<String, Object>> response = ApiResponse.success(message, data);
            return ResponseEntity.ok(response);
    }

    /**
     * Tạo giao dịch chuyển khoản ngân hàng
     * POST /api/v1/payments/bank-transfer/create
     */
    @PostMapping("/bank-transfer/create")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createBankTransferTransaction(
            @RequestBody Map<String, Object> request) {
        UserEntity user = getCurrentUser();

        // Lấy số tiền từ request
        Object amountObj = request.get("amount");
        Double amount = null;

        if (amountObj instanceof Integer) {
            amount = ((Integer) amountObj).doubleValue();
        } else if (amountObj instanceof Double) {
            amount = (Double) amountObj;
        } else if (amountObj instanceof String) {
            amount = Double.parseDouble((String) amountObj);
        }

        if (amount == null || amount <= 0) {
            throw new InvalidParamException("Số tiền không hợp lệ");
        }

        Map<String, Object> data = bankTransferService.createBankTransferTransaction(user.getId(), amount);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.PAYMENT_BANK_TRANSFER_CREATE_SUCCESS);
        ApiResponse<Map<String, Object>> response = ApiResponse.success(message, data);
        return ResponseEntity.ok(response);
    }

    /**
     * Verify chuyển khoản ngân hàng (Admin hoặc System)
     * POST /api/v1/payments/bank-transfer/verify
     */
    @PostMapping("/bank-transfer/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyBankTransfer(
            @RequestBody Map<String, String> request) {
        String transactionCode = request.get("transactionCode");
        if (transactionCode == null || transactionCode.isEmpty()) {
            throw new InvalidParamException("Mã giao dịch không được để trống");
        }

        // Lấy user hiện tại để check quyền (có thể thêm check ADMIN role)
        UserEntity currentUser = getCurrentUser();
        String verifiedBy = "USER_" + currentUser.getId(); // Có thể thêm role check

        Map<String, Object> data = bankTransferService.verifyBankTransfer(transactionCode, verifiedBy);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.PAYMENT_BANK_TRANSFER_VERIFY_SUCCESS);
        ApiResponse<Map<String, Object>> response = ApiResponse.success(message, data);
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}

