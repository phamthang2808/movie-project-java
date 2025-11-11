package com.example.thangcachep.movie_project_be.models.responses;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponse {

    private Long id;
    private String content;
    private Long parentId;
    private String author; // Tên user hoặc "Ẩn danh"
    private String avatar; // Avatar URL hoặc null
    private Integer likeCount;
    private Boolean isLiked; // true nếu user hiện tại đã like comment này
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommentResponse> replies; // Danh sách replies

    // Fields for Admin/Staff management
    private String status; // PENDING, APPROVED, REJECTED
    private Long userId; // ID của user tạo comment
    private String userEmail; // Email của user
    private Long movieId; // ID của phim
    private String movieTitle; // Tên phim
    private Long approvedById; // ID của admin duyệt/từ chối
    private String approvedByName; // Tên của admin duyệt/từ chối
    private LocalDateTime approvedAt; // Thời gian duyệt/từ chối
}

