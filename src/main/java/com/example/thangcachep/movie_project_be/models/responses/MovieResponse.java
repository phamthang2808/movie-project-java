package com.example.thangcachep.movie_project_be.models.responses;

import java.time.LocalDate;
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
public class MovieResponse {

    private Long id;
    private String title;
    private String description;
    private String posterUrl;
    private String backdropUrl;
    private String trailerUrl;
    private String videoUrl;
    private String type; // MOVIE, SERIES
    private String status; // PENDING, UPCOMING, AIRING, COMPLETED, REJECTED
    private Integer duration;
    private LocalDate releaseDate;
    private Double rating;
    private Integer ratingCount;
    private Integer viewCount;
    private Boolean isVipOnly;
    private Double price;
    private Boolean isActive;
    private List<String> categories;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Fields để track ai tạo và ai duyệt phim
    private Long createdById; // ID của user tạo phim
    private String createdByName; // Tên của user tạo phim
    private Long approvedById; // ID của admin duyệt phim
    private String approvedByName; // Tên của admin duyệt phim
    private LocalDateTime approvedAt; // Thời gian duyệt
}


