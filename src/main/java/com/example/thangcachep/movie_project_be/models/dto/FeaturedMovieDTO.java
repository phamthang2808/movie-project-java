package com.example.thangcachep.movie_project_be.models.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho Featured Movie Response
 * Sử dụng MovieDTO riêng để tránh phụ thuộc vào MovieResponse của backend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeaturedMovieDTO {
    private Long id;
    private Long movieId;
    private Integer displayOrder;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Sử dụng MovieDTO riêng (không dùng MovieResponse để độc lập)
    private MovieDTO movie;
}

