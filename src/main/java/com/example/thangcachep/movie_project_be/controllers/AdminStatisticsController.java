package com.example.thangcachep.movie_project_be.controllers;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.thangcachep.movie_project_be.models.responses.StatisticsResponse;
import com.example.thangcachep.movie_project_be.services.impl.StatisticsService;

import lombok.RequiredArgsConstructor;

/**
 * Admin Statistics Controller
 * Quyền hạn: Chỉ ADMIN mới có quyền xem thống kê toàn hệ thống
 */
@RestController
@RequestMapping("/api/v1/admin/statistics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatisticsController {

    private final StatisticsService statisticsService;

    /**
     * Lấy thống kê toàn hệ thống cho Admin
     * GET /api/v1/admin/statistics
     *
     * Query parameters:
     * - month: Tháng (1-12), nếu không có thì dùng tháng hiện tại
     * - year: Năm (ví dụ: 2024), nếu không có thì dùng năm hiện tại
     * - startDate: Ngày bắt đầu (format: yyyy-MM-dd), nếu có thì bỏ qua month/year
     * - endDate: Ngày kết thúc (format: yyyy-MM-dd), nếu có thì bỏ qua month/year
     *
     * @return StatisticsResponse chứa:
     * - totalMovies: Tổng số phim
     * - totalUsers: Tổng số người dùng
     * - monthlyRevenue: Doanh thu trong khoảng thời gian
     * - totalComments: Tổng số bình luận
     * - moviesChange, usersChange, revenueChange, commentsChange: % thay đổi so với khoảng thời gian trước
     */
    @GetMapping
    public ResponseEntity<StatisticsResponse> getStatistics(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {
            System.out.println("📊 Admin Statistics API called with params:");
            System.out.println("  - month: " + month);
            System.out.println("  - year: " + year);
            System.out.println("  - startDate: " + startDate);
            System.out.println("  - endDate: " + endDate);

            StatisticsResponse statistics = statisticsService.getAdminStatistics(month, year, startDate, endDate);

            System.out.println("📊 Admin Statistics Response:");
            System.out.println("  - totalMovies: " + statistics.getTotalMovies());
            System.out.println("  - monthlyRevenue: " + statistics.getMonthlyRevenue());

            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            // Log error
            System.err.println("Error getting admin statistics: " + e.getMessage());
            e.printStackTrace();
            // Trả về response rỗng với giá trị 0
            StatisticsResponse emptyStats = StatisticsResponse.builder()
                    .totalMovies(0L)
                    .totalUsers(0L)
                    .monthlyRevenue(0L)
                    .totalComments(0L)
                    .moviesChange(0.0)
                    .usersChange(0.0)
                    .revenueChange(0.0)
                    .commentsChange(0.0)
                    .build();
            return ResponseEntity.ok(emptyStats);
        }
    }
}

