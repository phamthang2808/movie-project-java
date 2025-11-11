package com.example.thangcachep.movie_project_be.entities;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity cho bảng Movies
 * Lưu trữ thông tin phim: phim lẻ và phim bộ
 */
@Entity
@Table(name = "movies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieEntity extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(length = 500)
    private String posterUrl;

    @Column(length = 500)
    private String backdropUrl;

    @Column(length = 500)
    private String trailerUrl;

    @Column(length = 500)
    private String videoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovieType type = MovieType.MOVIE;

    @Column(nullable = false)
    private Integer duration; // phút

    @Column
    private LocalDate releaseDate;

    @Column(nullable = false)
    private Double rating = 0.0;

    @Column(nullable = false)
    private Integer ratingCount = 0;

    @Column(nullable = false)
    private Integer viewCount = 0;

    @Column(nullable = false)
    private Boolean isVipOnly = false;

    @Column(nullable = false)
    private Double price = 0.0;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovieStatus status = MovieStatus.UPCOMING;

    // Relationships
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "movie_categories",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<CategoryEntity> categories = new HashSet<>();

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<EpisodeEntity> episodes = new HashSet<>();

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CommentEntity> comments = new HashSet<>();

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RatingEntity> ratings = new HashSet<>();

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<FavoriteEntity> favorites = new HashSet<>();

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<WatchlistEntity> watchlists = new HashSet<>();

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<WatchHistoryEntity> watchHistories = new HashSet<>();

    // Enum cho MovieType
    public enum MovieType {
        MOVIE, SERIES
    }

    // Fields để track ai tạo và ai duyệt phim
    @ManyToOne
    @JoinColumn(name = "created_by_user_id")
    private UserEntity createdBy; // Staff/Admin nào tạo phim này

    @ManyToOne
    @JoinColumn(name = "approved_by_user_id")
    private UserEntity approvedBy; // Admin nào duyệt phim này

    @Column
    private java.time.LocalDateTime approvedAt; // Thời gian duyệt

    // Enum cho MovieStatus
    public enum MovieStatus {
        PENDING,    // Chờ duyệt (phim do Staff tạo, chưa được Admin duyệt)
        UPCOMING,   // Sắp chiếu
        AIRING,     // Đang chiếu
        COMPLETED,  // Hoàn thành
        REJECTED    // Đã từ chối (phim bị Admin reject)
    }
}
