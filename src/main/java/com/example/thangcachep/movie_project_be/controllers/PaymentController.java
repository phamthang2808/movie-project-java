package com.example.thangcachep.movie_project_be.controllers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
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

import com.example.thangcachep.movie_project_be.entities.TransactionEntity;
import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.models.responses.UserResponse;
import com.example.thangcachep.movie_project_be.repositories.TransactionRepository;
import com.example.thangcachep.movie_project_be.repositories.UserRepository;
import com.example.thangcachep.movie_project_be.services.impl.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final TransactionRepository transactionRepository;

    /**
     * Mua VIP - Check balance, trừ tiền, update VIP expiration
     */
    @PostMapping("/upgrade-vip")
    public ResponseEntity<Map<String, Object>> upgradeToVip(@RequestBody Map<String, Object> request) {
        try {
            UserEntity user = getCurrentUser();

            // Lấy thông tin từ request
            Integer planId = (Integer) request.get("planId");
            Integer price = (Integer) request.get("price");

            if (planId == null || price == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Thiếu thông tin gói VIP"));
            }

            // Kiểm tra số dư
            if (user.getBalance() < price) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Số dư không đủ! Vui lòng nạp thêm tiền."));
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

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Nâng cấp VIP thành công!");
            response.put("user", userResponse);
            response.put("transaction", Map.of(
                    "id", transaction.getId(),
                    "amount", transaction.getAmount(),
                    "type", transaction.getType().name()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Lỗi khi mua VIP: " + e.getMessage()));
        }
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
    public ResponseEntity<Map<String, Object>> getTransactionHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
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

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transactions", transactionList);
            response.put("total", total);
            response.put("page", page);
            response.put("size", size);
            response.put("totalPages", (int) Math.ceil((double) total / size));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Lỗi khi lấy lịch sử giao dịch: " + e.getMessage()));
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}

