package com.example.thangcachep.movie_project_be.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity cho bảng Featured Movies
 * Quản lý 6 phim nổi bật hiển thị ở trang chủ
 */
@Entity
@Table(name = "featured_movies",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "movie_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeaturedMovieEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private MovieEntity movie;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder; // 1-6, 1 là phim chính

    @Column(nullable = false)
    private Boolean isActive = true;
}

