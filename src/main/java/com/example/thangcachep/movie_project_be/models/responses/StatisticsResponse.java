package com.example.thangcachep.movie_project_be.models.responses;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Statistics Response DTO
 * Dùng cho cả Admin và Staff statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonTypeName("StatisticsResponse")
public class StatisticsResponse {

    // ========== Admin Statistics ==========
    // Tổng số phim trong hệ thống
    private Long totalMovies;

    // Tổng số người dùng
    private Long totalUsers;

    // Doanh thu tháng hiện tại (VND)
    private Long monthlyRevenue;

    // Tổng số bình luận
    private Long totalComments;

    // % thay đổi so với tháng trước
    private Double moviesChange;      // % thay đổi số phim
    private Double usersChange;        // % thay đổi số người dùng
    private Double revenueChange;      // % thay đổi doanh thu
    private Double commentsChange;     // % thay đổi số bình luận

    // ========== Staff Statistics ==========
    // Số phim đang quản lý (chỉ phim do Staff tạo)
    private Long managedMovies;

    // Số bình luận chờ duyệt
    private Long pendingComments;

    // Số báo cáo từ người dùng
    private Long userReports;

    // Lượt xem hôm nay (tổng lượt xem của các phim do Staff quản lý)
    private Long todayViews;

    // Thay đổi so với trước đó (có thể là số tuyệt đối hoặc %)
    private Long managedMoviesChange;      // Số phim thay đổi
    private Long pendingCommentsChange;    // Số bình luận thay đổi
    private Long userReportsChange;        // Số báo cáo thay đổi
    private Double todayViewsChange;       // % thay đổi lượt xem

    // Thông tin bổ sung (optional)
    private String recentMovieTitle;       // Tên phim gần đây nhất (cho Staff)
}

