package com.example.thangcachep.movie_project_be.models.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import lombok.*;

@Getter
@Setter
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

