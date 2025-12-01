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
 * Staff Statistics Controller
 * Quyền hạn: STAFF có thể xem thống kê liên quan đến công việc của mình
 */
@RestController
@RequestMapping("/api/v1/staff/statistics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')") // Admin cũng có thể xem staff stats
public class StaffStatisticsController {

    private final StatisticsService statisticsService;

    /**
     * Lấy thống kê cho Staff (chỉ liên quan đến công việc của Staff)
     * GET /api/v1/staff/statistics
     *
     * Query parameters:
     * - month: Tháng (1-12), nếu không có thì dùng tháng hiện tại
     * - year: Năm (ví dụ: 2024), nếu không có thì dùng năm hiện tại
     * - startDate: Ngày bắt đầu (format: yyyy-MM-dd), nếu có thì bỏ qua month/year
     * - endDate: Ngày kết thúc (format: yyyy-MM-dd), nếu có thì bỏ qua month/year
     *
     * @return StatisticsResponse chứa:
     * - managedMovies: Số phim đang quản lý (phim do Staff tạo)
     * - pendingComments: Số bình luận chờ duyệt
     * - userReports: Số báo cáo từ người dùng
     * - todayViews: Lượt xem trong khoảng thời gian (của các phim do Staff quản lý)
     * - managedMoviesChange, pendingCommentsChange, userReportsChange, todayViewsChange: Thay đổi
     * - recentMovieTitle: Tên phim gần đây nhất
     */
    @GetMapping
    public ResponseEntity<StatisticsResponse> getStatistics(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {
            System.out.println("📊 Staff Statistics API called with params:");
            System.out.println("  - month: " + month);
            System.out.println("  - year: " + year);
            System.out.println("  - startDate: " + startDate);
            System.out.println("  - endDate: " + endDate);

            StatisticsResponse statistics = statisticsService.getStaffStatistics(month, year, startDate, endDate);

            System.out.println("📊 Staff Statistics Response:");
            System.out.println("  - managedMovies: " + statistics.getManagedMovies());
            System.out.println("  - todayViews: " + statistics.getTodayViews());

            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            // Log error
            System.err.println("Error getting staff statistics: " + e.getMessage());
            e.printStackTrace();
            // Trả về response rỗng với giá trị 0
            StatisticsResponse emptyStats = StatisticsResponse.builder()
                    .managedMovies(0L)
                    .pendingComments(0L)
                    .userReports(0L)
                    .todayViews(0L)
                    .managedMoviesChange(0L)
                    .pendingCommentsChange(0L)
                    .userReportsChange(0L)
                    .todayViewsChange(0.0)
                    .recentMovieTitle("N/A")
                    .build();
            return ResponseEntity.ok(emptyStats);
        }
    }
}

