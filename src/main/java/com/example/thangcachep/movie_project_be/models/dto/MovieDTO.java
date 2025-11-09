package com.example.thangcachep.movie_project_be.models.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho Movie Response
 * Dùng chung cho các API trả về thông tin phim
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieDTO {
    private Long id;
    private String title;
    private String description;
    private String posterUrl;
    private String backdropUrl;
    private String trailerUrl;
    private String videoUrl;
    private String type; // MOVIE, SERIES
    private Integer duration; // phút
    private LocalDate releaseDate;
    private Double rating;
    private Integer ratingCount;
    private Integer viewCount;
    private Boolean isVipOnly;
    private Double price;
    private Boolean isActive;
    private String status; // UPCOMING, AIRING, COMPLETED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Optional: Vote average (có thể dùng rating hoặc tính toán riêng)
    // Note: Nếu cần categories, có thể thêm sau hoặc tạo MovieDetailDTO riêng
    private Double voteAverage;
}

