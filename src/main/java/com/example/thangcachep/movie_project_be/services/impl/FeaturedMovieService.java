package com.example.thangcachep.movie_project_be.services.impl;


import com.example.thangcachep.movie_project_be.entities.FeaturedMovieEntity;
import com.example.thangcachep.movie_project_be.entities.MovieEntity;
import com.example.thangcachep.movie_project_be.models.dto.FeaturedMovieDTO;
import com.example.thangcachep.movie_project_be.models.dto.MovieDTO;
import com.example.thangcachep.movie_project_be.repositories.FeaturedMovieRepository;
import com.example.thangcachep.movie_project_be.services.IFeaturedMovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service Implementation cho Featured Movies
 * Chỉ có logic để lấy danh sách 6 phim nổi bật
 */
@Service
@RequiredArgsConstructor
public class FeaturedMovieService implements IFeaturedMovieService {

    private final FeaturedMovieRepository featuredMovieRepository;

    /**
     * Get featured movies - Cache 1 giờ
     * Cache key: "featured:movies"
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "movies", key = "'featured:movies'")
    public List<FeaturedMovieDTO> getFeaturedMovies() {
        List<FeaturedMovieEntity> entities = featuredMovieRepository.findAllActiveOrderByDisplayOrder();
        return entities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convert Entity sang DTO
     */
    private FeaturedMovieDTO convertToDTO(FeaturedMovieEntity entity) {
        MovieEntity movieEntity = entity.getMovie();

        // Convert MovieEntity sang MovieDTO
        MovieDTO movieDTO = convertMovieEntityToDTO(movieEntity);

        return FeaturedMovieDTO.builder()
                .id(entity.getId())
                .movieId(movieEntity.getId())
                .displayOrder(entity.getDisplayOrder())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .movie(movieDTO)
                .build();
    }

    /**
     * Convert MovieEntity sang MovieDTO
     */
    private MovieDTO convertMovieEntityToDTO(MovieEntity movieEntity) {
        // Tính toán status dựa trên releaseDate và isActive
        String status = calculateMovieStatus(movieEntity);

        return MovieDTO.builder()
                .id(movieEntity.getId())
                .title(movieEntity.getTitle())
                .description(movieEntity.getDescription())
                .posterUrl(movieEntity.getPosterUrl())
                .backdropUrl(movieEntity.getBackdropUrl())
                .trailerUrl(movieEntity.getTrailerUrl())
                .videoUrl(movieEntity.getVideoUrl())
                .type(movieEntity.getType() != null ? movieEntity.getType().name() : null)
                .duration(movieEntity.getDuration())
                .releaseDate(movieEntity.getReleaseDate())
                .rating(movieEntity.getRating())
                .ratingCount(movieEntity.getRatingCount())
                .viewCount(movieEntity.getViewCount())
                .isVipOnly(movieEntity.getIsVipOnly())
                .price(movieEntity.getPrice())
                .isActive(movieEntity.getIsActive())
                .status(status)
                .voteAverage(movieEntity.getRating())
                .createdAt(movieEntity.getCreatedAt())
                .updatedAt(movieEntity.getUpdatedAt())
                .build();
    }

    /**
     * Tính toán status của phim dựa trên releaseDate và isActive
     */
    private String calculateMovieStatus(MovieEntity movieEntity) {
        if (movieEntity.getReleaseDate() == null) {
            return "UPCOMING";
        }

        java.time.LocalDate today = java.time.LocalDate.now();
        if (movieEntity.getReleaseDate().isAfter(today)) {
            return "UPCOMING";
        } else if (movieEntity.getReleaseDate().isBefore(today) || movieEntity.getReleaseDate().isEqual(today)) {
            if (Boolean.TRUE.equals(movieEntity.getIsActive())) {
                return "AIRING";
            } else {
                return "UPCOMING";
            }
        }
        return "UPCOMING";
    }
}
