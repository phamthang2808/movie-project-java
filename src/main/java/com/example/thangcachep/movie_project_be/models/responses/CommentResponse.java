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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommentResponse> replies; // Danh sách replies
}

