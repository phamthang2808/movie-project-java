package com.example.thangcachep.movie_project_be.services.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.thangcachep.movie_project_be.entities.CommentEntity;
import com.example.thangcachep.movie_project_be.entities.MovieEntity;
import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.models.responses.StatisticsResponse;
import com.example.thangcachep.movie_project_be.repositories.CommentRepository;
import com.example.thangcachep.movie_project_be.repositories.MovieRepository;
import com.example.thangcachep.movie_project_be.repositories.ReportRepository;
import com.example.thangcachep.movie_project_be.repositories.TransactionRepository;
import com.example.thangcachep.movie_project_be.repositories.UserRepository;
import com.example.thangcachep.movie_project_be.repositories.WatchHistoryRepository;

import lombok.RequiredArgsConstructor;

/**
 * Statistics Service
 * Tính toán thống kê cho Admin và Staff
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final MovieRepository movieRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final TransactionRepository transactionRepository;
    private final ReportRepository reportRepository;
    private final WatchHistoryRepository watchHistoryRepository;
    private final CacheManager cacheManager;

    /**
     * Lấy thống kê cho Admin (toàn hệ thống)
     * Cache 5 phút - dữ liệu thống kê thay đổi thường xuyên
     *
     * @param month Tháng (1-12), null nếu dùng tháng hiện tại
     * @param year Năm, null nếu dùng năm hiện tại
     * @param startDate Ngày bắt đầu, nếu có thì bỏ qua month/year
     * @param endDate Ngày kết thúc, nếu có thì bỏ qua month/year
     */
    @Transactional(readOnly = true)
    @CacheEvict(value = "statistics", allEntries = true, beforeInvocation = true) // Xóa cache cũ trước khi cache mới
    @Cacheable(value = "statistics", key = "'admin:' + (#month != null ? #month : 'current') + ':' + (#year != null ? #year : 'current') + ':' + (#startDate != null ? #startDate.toString() : 'null') + ':' + (#endDate != null ? #endDate.toString() : 'null')")
    public StatisticsResponse getAdminStatistics(Integer month, Integer year, LocalDate startDate, LocalDate endDate) {
        System.out.println("🔧 StatisticsService.getAdminStatistics called with:");
        System.out.println("  month=" + month + ", year=" + year + ", startDate=" + startDate + ", endDate=" + endDate);

        // Thời gian hiện tại
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        // Xác định khoảng thời gian cần tính toán
        LocalDateTime startOfPeriod;
        LocalDateTime endOfPeriod;

        if (startDate != null && endDate != null) {
            // Nếu có startDate và endDate, dùng khoảng thời gian này
            startOfPeriod = startDate.atStartOfDay();
            endOfPeriod = endDate.atTime(23, 59, 59);
            System.out.println("✅ Using custom date range: " + startOfPeriod + " to " + endOfPeriod);
        } else if (month != null && year != null) {
            // Nếu có month và year, tính theo tháng/năm đó
            LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
            LocalDate lastDayOfMonth = firstDayOfMonth.withDayOfMonth(firstDayOfMonth.lengthOfMonth());
            startOfPeriod = firstDayOfMonth.atStartOfDay();
            endOfPeriod = lastDayOfMonth.atTime(23, 59, 59);
            System.out.println("✅ Using month/year: " + startOfPeriod + " to " + endOfPeriod);
        } else {
            // Mặc định: tháng hiện tại
            LocalDate firstDayOfCurrentMonth = today.withDayOfMonth(1);
            LocalDate lastDayOfCurrentMonth = firstDayOfCurrentMonth.withDayOfMonth(firstDayOfCurrentMonth.lengthOfMonth());
            startOfPeriod = firstDayOfCurrentMonth.atStartOfDay();
            endOfPeriod = lastDayOfCurrentMonth.atTime(23, 59, 59);
            System.out.println("✅ Using current month (default): " + startOfPeriod + " to " + endOfPeriod);
        }

        // Tính khoảng thời gian trước (cùng độ dài)
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startOfPeriod.toLocalDate(), endOfPeriod.toLocalDate());
        LocalDateTime startOfPreviousPeriod = startOfPeriod.minusDays(daysBetween + 1);
        LocalDateTime endOfPreviousPeriod = startOfPeriod.minusSeconds(1);

        // ========== Tính toán số liệu trong khoảng thời gian ==========
        // Tổng phim được tạo trong khoảng thời gian (chỉ tính phim active)
        long totalMovies = movieRepository.findAll().stream()
                .filter(m -> m.getIsActive() != null && m.getIsActive() &&
                        m.getCreatedAt() != null &&
                        !m.getCreatedAt().isBefore(startOfPeriod) &&
                        !m.getCreatedAt().isAfter(endOfPeriod))
                .count();

        // Tổng người dùng được tạo trong khoảng thời gian (chỉ tính user active)
        long totalUsers = userRepository.findAll().stream()
                .filter(u -> u.getIsActive() != null && u.getIsActive() &&
                        u.getCreatedAt() != null &&
                        !u.getCreatedAt().isBefore(startOfPeriod) &&
                        !u.getCreatedAt().isAfter(endOfPeriod))
                .count();

        // Doanh thu trong khoảng thời gian (chỉ tính từ nạp tiền - RECHARGE)
        long monthlyRevenue = transactionRepository.findAll().stream()
                .filter(t -> t.getCreatedAt() != null &&
                        !t.getCreatedAt().isBefore(startOfPeriod) &&
                        !t.getCreatedAt().isAfter(endOfPeriod) &&
                        t.getStatus() == com.example.thangcachep.movie_project_be.entities.TransactionEntity.TransactionStatus.COMPLETED &&
                        t.getType() == com.example.thangcachep.movie_project_be.entities.TransactionEntity.TransactionType.RECHARGE)
                .mapToLong(t -> t.getAmount() != null ? t.getAmount().longValue() : 0L)
                .sum();

        // Tổng bình luận được tạo trong khoảng thời gian (chỉ tính comment active)
        long totalComments = commentRepository.findAll().stream()
                .filter(c -> c.getIsActive() != null && c.getIsActive() &&
                        c.getCreatedAt() != null &&
                        !c.getCreatedAt().isBefore(startOfPeriod) &&
                        !c.getCreatedAt().isAfter(endOfPeriod))
                .count();

        // ========== Tính toán số liệu khoảng thời gian trước ==========
        // Đếm phim được tạo trong khoảng thời gian trước
        long moviesPreviousPeriod = movieRepository.findAll().stream()
                .filter(m -> m.getCreatedAt() != null &&
                        !m.getCreatedAt().isBefore(startOfPreviousPeriod) &&
                        !m.getCreatedAt().isAfter(endOfPreviousPeriod))
                .count();

        // Đếm user được tạo trong khoảng thời gian trước
        long usersPreviousPeriod = userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt() != null &&
                        !u.getCreatedAt().isBefore(startOfPreviousPeriod) &&
                        !u.getCreatedAt().isAfter(endOfPreviousPeriod))
                .count();

        // Doanh thu khoảng thời gian trước (chỉ tính từ nạp tiền - RECHARGE)
        long revenuePreviousPeriod = transactionRepository.findAll().stream()
                .filter(t -> t.getCreatedAt() != null &&
                        !t.getCreatedAt().isBefore(startOfPreviousPeriod) &&
                        !t.getCreatedAt().isAfter(endOfPreviousPeriod) &&
                        t.getStatus() == com.example.thangcachep.movie_project_be.entities.TransactionEntity.TransactionStatus.COMPLETED &&
                        t.getType() == com.example.thangcachep.movie_project_be.entities.TransactionEntity.TransactionType.RECHARGE)
                .mapToLong(t -> t.getAmount() != null ? t.getAmount().longValue() : 0L)
                .sum();

        // Đếm comment được tạo trong khoảng thời gian trước
        long commentsPreviousPeriod = commentRepository.findAll().stream()
                .filter(c -> c.getCreatedAt() != null &&
                        !c.getCreatedAt().isBefore(startOfPreviousPeriod) &&
                        !c.getCreatedAt().isAfter(endOfPreviousPeriod))
                .count();

        // ========== Tính % thay đổi ==========
        double moviesChange = calculatePercentageChange(moviesPreviousPeriod, totalMovies);
        double usersChange = calculatePercentageChange(usersPreviousPeriod, totalUsers);
        double revenueChange = calculatePercentageChange(revenuePreviousPeriod, monthlyRevenue);
        double commentsChange = calculatePercentageChange(commentsPreviousPeriod, totalComments);

        return StatisticsResponse.builder()
                .totalMovies(totalMovies)
                .totalUsers(totalUsers)
                .monthlyRevenue(monthlyRevenue)
                .totalComments(totalComments)
                .moviesChange(moviesChange)
                .usersChange(usersChange)
                .revenueChange(revenueChange)
                .commentsChange(commentsChange)
                .build();
    }

    /**
     * Lấy thống kê cho Staff (chỉ liên quan đến công việc của Staff)
     * Cache 5 phút - dữ liệu thống kê thay đổi thường xuyên
     *
     * @param month Tháng (1-12), null nếu dùng tháng hiện tại
     * @param year Năm, null nếu dùng năm hiện tại
     * @param startDate Ngày bắt đầu, nếu có thì bỏ qua month/year
     * @param endDate Ngày kết thúc, nếu có thì bỏ qua month/year
     */
    @Transactional(readOnly = true)
    @CacheEvict(value = "statistics", allEntries = true, beforeInvocation = true) // Xóa cache cũ trước khi cache mới
    @Cacheable(value = "statistics", key = "'staff:' + (#month != null ? #month : 'current') + ':' + (#year != null ? #year : 'current') + ':' + (#startDate != null ? #startDate.toString() : 'null') + ':' + (#endDate != null ? #endDate.toString() : 'null') + ':' + T(java.util.Objects).hash(T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getPrincipal())")
    public StatisticsResponse getStaffStatistics(Integer month, Integer year, LocalDate startDate, LocalDate endDate) {
        // Lấy thông tin user hiện tại
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();
        UserEntity currentUser;
        if (principal instanceof UserEntity) {
            currentUser = (UserEntity) principal;
        } else {
            // Nếu không phải UserEntity, không thể lấy được user
            throw new RuntimeException("Invalid authentication principal");
        }
        Long staffId = currentUser.getId();

        // Thời gian hiện tại
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        // Xác định khoảng thời gian cần tính toán
        LocalDateTime startOfPeriod;
        LocalDateTime endOfPeriod;

        if (startDate != null && endDate != null) {
            // Nếu có startDate và endDate, dùng khoảng thời gian này
            startOfPeriod = startDate.atStartOfDay();
            endOfPeriod = endDate.atTime(23, 59, 59);
        } else if (month != null && year != null) {
            // Nếu có month và year, tính theo tháng/năm đó
            LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
            LocalDate lastDayOfMonth = firstDayOfMonth.withDayOfMonth(firstDayOfMonth.lengthOfMonth());
            startOfPeriod = firstDayOfMonth.atStartOfDay();
            endOfPeriod = lastDayOfMonth.atTime(23, 59, 59);
        } else {
            // Mặc định: hôm nay
            startOfPeriod = today.atStartOfDay();
            endOfPeriod = today.atTime(23, 59, 59);
        }

        // ========== Tính toán số liệu ==========
        // Phim đang quản lý (phim do Staff này tạo)
        long managedMovies = movieRepository.findByCreatedByIdWithCategories(staffId).size();

        // Bình luận chờ duyệt (tất cả comment PENDING)
        long pendingComments = commentRepository.findByStatus(CommentEntity.CommentStatus.PENDING).size();

        // Báo cáo từ người dùng (tất cả report chưa xử lý)
        long userReports = reportRepository.findAll().stream()
                .filter(r -> r.getStatus() == com.example.thangcachep.movie_project_be.entities.ReportEntity.ReportStatus.PENDING ||
                        r.getStatus() == com.example.thangcachep.movie_project_be.entities.ReportEntity.ReportStatus.PROCESSING)
                .count();

        // Lượt xem trong khoảng thời gian (tổng viewCount của các phim do Staff quản lý)
        // Lấy danh sách phim do Staff tạo
        var staffMovies = movieRepository.findByCreatedByIdWithCategories(staffId);
        long periodViews = staffMovies.stream()
                .filter(m -> m.getUpdatedAt() != null &&
                        !m.getUpdatedAt().isBefore(startOfPeriod) &&
                        !m.getUpdatedAt().isAfter(endOfPeriod))
                .mapToLong(m -> m.getViewCount() != null ? m.getViewCount() : 0L)
                .sum();

        // Nếu không có view trong khoảng thời gian, tính tổng viewCount của tất cả phim do Staff quản lý
        if (periodViews == 0) {
            periodViews = staffMovies.stream()
                    .mapToLong(m -> m.getViewCount() != null ? m.getViewCount() : 0L)
                    .sum();
        }

        // ========== Tính thay đổi (so với khoảng thời gian trước) ==========
        // Tính khoảng thời gian trước (cùng độ dài)
        long daysBetween = ChronoUnit.DAYS.between(startOfPeriod.toLocalDate(), endOfPeriod.toLocalDate());
        LocalDateTime startOfPreviousPeriod = startOfPeriod.minusDays(daysBetween + 1);
        LocalDateTime endOfPreviousPeriod = startOfPeriod.minusSeconds(1);

        // Tính thay đổi số phim (so với khoảng thời gian trước)
        long managedMoviesPreviousPeriod = movieRepository.findByCreatedByIdWithCategories(staffId).stream()
                .filter(m -> m.getCreatedAt() != null &&
                        !m.getCreatedAt().isBefore(startOfPreviousPeriod) &&
                        !m.getCreatedAt().isAfter(endOfPreviousPeriod))
                .count();
        long managedMoviesChange = managedMovies - managedMoviesPreviousPeriod;

        // Tính thay đổi bình luận chờ duyệt (so với khoảng thời gian trước)
        long pendingCommentsPreviousPeriod = commentRepository.findAll().stream()
                .filter(c -> c.getStatus() == CommentEntity.CommentStatus.PENDING &&
                        c.getCreatedAt() != null &&
                        !c.getCreatedAt().isBefore(startOfPreviousPeriod) &&
                        !c.getCreatedAt().isAfter(endOfPreviousPeriod))
                .count();
        long pendingCommentsChange = pendingComments - pendingCommentsPreviousPeriod;

        // Tính thay đổi báo cáo (so với khoảng thời gian trước)
        long userReportsPreviousPeriod = reportRepository.findAll().stream()
                .filter(r -> (r.getStatus() == com.example.thangcachep.movie_project_be.entities.ReportEntity.ReportStatus.PENDING ||
                        r.getStatus() == com.example.thangcachep.movie_project_be.entities.ReportEntity.ReportStatus.PROCESSING) &&
                        r.getCreatedAt() != null &&
                        !r.getCreatedAt().isBefore(startOfPreviousPeriod) &&
                        !r.getCreatedAt().isAfter(endOfPreviousPeriod))
                .count();
        long userReportsChange = userReports - userReportsPreviousPeriod;

        // Tính % thay đổi lượt xem (so với khoảng thời gian trước)
        long previousPeriodViews = staffMovies.stream()
                .filter(m -> m.getUpdatedAt() != null &&
                        !m.getUpdatedAt().isBefore(startOfPreviousPeriod) &&
                        !m.getUpdatedAt().isAfter(endOfPreviousPeriod))
                .mapToLong(m -> m.getViewCount() != null ? m.getViewCount() : 0L)
                .sum();
        double periodViewsChange = calculatePercentageChange(previousPeriodViews, periodViews);

        // Lấy tên phim gần đây nhất
        String recentMovieTitle = staffMovies.stream()
                .max((m1, m2) -> {
                    if (m1.getCreatedAt() == null) return -1;
                    if (m2.getCreatedAt() == null) return 1;
                    return m1.getCreatedAt().compareTo(m2.getCreatedAt());
                })
                .map(MovieEntity::getTitle)
                .orElse("N/A");

        return StatisticsResponse.builder()
                .managedMovies(managedMovies)
                .pendingComments(pendingComments)
                .userReports(userReports)
                .todayViews(periodViews)
                .managedMoviesChange(managedMoviesChange)
                .pendingCommentsChange(pendingCommentsChange)
                .userReportsChange(userReportsChange)
                .todayViewsChange(periodViewsChange)
                .recentMovieTitle(recentMovieTitle)
                .build();
    }

    /**
     * Tính % thay đổi
     */
    private double calculatePercentageChange(long oldValue, long newValue) {
        if (oldValue == 0) {
            return newValue > 0 ? 100.0 : 0.0;
        }
        return ((double) (newValue - oldValue) / oldValue) * 100.0;
    }
}

