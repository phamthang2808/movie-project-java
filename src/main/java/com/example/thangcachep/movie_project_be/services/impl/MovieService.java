package com.example.thangcachep.movie_project_be.services.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.thangcachep.movie_project_be.entities.CategoryEntity;
import com.example.thangcachep.movie_project_be.entities.MovieEntity;
import com.example.thangcachep.movie_project_be.exceptions.DataNotFoundException;
import com.example.thangcachep.movie_project_be.models.request.MovieRequest;
import com.example.thangcachep.movie_project_be.models.responses.MovieResponse;
import com.example.thangcachep.movie_project_be.repositories.CategoryRepository;
import com.example.thangcachep.movie_project_be.repositories.MovieRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final CategoryRepository categoryRepository;

    public Page<MovieResponse> getAllMovies(String type, Long categoryId, Boolean includeInactive, Pageable pageable) {
        Page<MovieEntity> movies;

        if (categoryId != null) {
            // findByCategoryId đã filter isActive = true
            movies = movieRepository.findByCategoryId(categoryId, pageable);
        } else if (type != null) {
            MovieEntity.MovieType movieType = MovieEntity.MovieType.valueOf(type.toUpperCase());
            // Nếu includeInactive = true, lấy cả phim inactive
            if (includeInactive != null && includeInactive) {
                movies = movieRepository.findByType(movieType, pageable);
            } else {
                // Mặc định chỉ lấy phim active
                movies = movieRepository.findByTypeAndIsActiveTrue(movieType, pageable);
            }
        } else {
            // Nếu includeInactive = true, lấy tất cả (kể cả inactive)
            if (includeInactive != null && includeInactive) {
                movies = movieRepository.findAll(pageable);
            } else {
                movies = movieRepository.findByIsActive(true, pageable);
            }
        }

        return movies.map(this::mapToMovieResponse);
    }

    /**
     * Lấy tất cả phim không phân trang (dùng cho admin)
     * Mặc định chỉ lấy phim active (isActive = true)
     * Nếu includeInactive = true, mới lấy cả phim đã xóa
     *
     * Tối ưu: Sử dụng JOIN FETCH để load categories cùng lúc, tránh N+1 query problem
     */
    public List<MovieResponse> getAllMoviesWithoutPagination(Boolean includeInactive) {
        List<MovieEntity> movies;

        // Mặc định chỉ lấy phim active (isActive = true)
        // Sử dụng query đã tối ưu với JOIN FETCH để load categories
        if (includeInactive != null && includeInactive) {
            // Nếu cần lấy cả phim đã xóa (soft delete)
            movies = movieRepository.findAllWithCategories();
        } else {
            // Chỉ lấy phim active với categories đã được fetch
            movies = movieRepository.findByIsActiveTrueWithCategories();
        }

        return movies.stream()
                .map(this::mapToMovieResponse)
                .collect(Collectors.toList());
    }

    public MovieResponse getMovieById(Long id) {
        // Sử dụng query tối ưu với JOIN FETCH để load categories cùng lúc
        MovieEntity movie = movieRepository.findByIdWithCategories(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phim với ID: " + id));
        return mapToMovieResponse(movie);
    }

    public Page<MovieResponse> searchMovies(String keyword, Pageable pageable) {
        Page<MovieEntity> movies = movieRepository.searchMovies(keyword, pageable);
        return movies.map(this::mapToMovieResponse);
    }

    public List<MovieResponse> getTrendingMovies() {
        List<MovieEntity> movies = movieRepository.findTopViewedMovies(PageRequest.of(0, 10));
        return movies.stream()
                .map(this::mapToMovieResponse)
                .collect(Collectors.toList());
    }

    public List<MovieResponse> getTopMoviesWeek() {
        List<MovieEntity> movies = movieRepository.findTopRatedMovies(PageRequest.of(0, 10));
        return movies.stream()
                .map(this::mapToMovieResponse)
                .collect(Collectors.toList());
    }

    public Page<MovieResponse> getMoviesByCategory(Long categoryId, Pageable pageable) {
        Page<MovieEntity> movies = movieRepository.findByCategoryId(categoryId, pageable);
        return movies.map(this::mapToMovieResponse);
    }

    /**
     * Tạo phim mới
     */
    @Transactional
    public MovieResponse createMovie(MovieRequest request) {
        MovieEntity movie = new MovieEntity();

        // Debug: Log request data
        System.out.println("Creating movie with data:");
        System.out.println("Title: " + request.getTitle());
        System.out.println("PosterUrl: " + request.getPosterUrl());
        System.out.println("BackdropUrl: " + request.getBackdropUrl());

        // Set các field cơ bản
        movie.setTitle(request.getTitle());
        movie.setDescription(request.getDescription());
        // Set URL - chỉ set nếu không null và không empty
        movie.setPosterUrl(request.getPosterUrl() != null && !request.getPosterUrl().trim().isEmpty()
                ? request.getPosterUrl().trim() : null);
        movie.setBackdropUrl(request.getBackdropUrl() != null && !request.getBackdropUrl().trim().isEmpty()
                ? request.getBackdropUrl().trim() : null);
        movie.setTrailerUrl(request.getTrailerUrl() != null && !request.getTrailerUrl().trim().isEmpty()
                ? request.getTrailerUrl().trim() : null);
        movie.setVideoUrl(request.getVideoUrl() != null && !request.getVideoUrl().trim().isEmpty()
                ? request.getVideoUrl().trim() : null);
        movie.setDuration(request.getDuration());
        movie.setReleaseDate(request.getReleaseDate());
        movie.setPrice(request.getPrice() != null ? request.getPrice() : 0.0);
        movie.setIsVipOnly(request.getIsVipOnly() != null ? request.getIsVipOnly() : false);

        // Set type
        if (request.getType() != null) {
            try {
                movie.setType(MovieEntity.MovieType.valueOf(request.getType().toUpperCase()));
            } catch (IllegalArgumentException e) {
                movie.setType(MovieEntity.MovieType.MOVIE);
            }
        } else {
            movie.setType(MovieEntity.MovieType.MOVIE);
        }

        // Set status
        if (request.getStatus() != null) {
            try {
                movie.setStatus(MovieEntity.MovieStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                movie.setStatus(MovieEntity.MovieStatus.UPCOMING);
            }
        } else {
            movie.setStatus(MovieEntity.MovieStatus.UPCOMING);
        }

        // Set categories
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            Set<CategoryEntity> categories = new HashSet<>();
            for (Long categoryId : request.getCategoryIds()) {
                categoryRepository.findById(categoryId).ifPresent(categories::add);
            }
            movie.setCategories(categories);
        }

        // Set default values
        movie.setRating(0.0);
        movie.setRatingCount(0);
        movie.setViewCount(0);
        movie.setIsActive(true);

        // Lưu vào database
        movie = movieRepository.save(movie);

        return mapToMovieResponse(movie);
    }

    /**
     * Cập nhật thông tin phim (bao gồm videoUrl)
     */
    @Transactional
    public MovieResponse updateMovie(Long id, Map<String, Object> updates) throws DataNotFoundException {
        MovieEntity movie = movieRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy phim với ID: " + id));

        // Debug: Log updates
        System.out.println("Updating movie " + id + " with data: " + updates);

        // Cập nhật các field từ Map
        if (updates.containsKey("title")) {
            movie.setTitle((String) updates.get("title"));
        }
        if (updates.containsKey("description")) {
            movie.setDescription((String) updates.get("description"));
        }
        if (updates.containsKey("posterUrl")) {
            String posterUrl = (String) updates.get("posterUrl");
            movie.setPosterUrl(posterUrl != null && !posterUrl.trim().isEmpty() ? posterUrl.trim() : null);
            System.out.println("Setting posterUrl: " + movie.getPosterUrl());
        }
        if (updates.containsKey("backdropUrl")) {
            String backdropUrl = (String) updates.get("backdropUrl");
            movie.setBackdropUrl(backdropUrl != null && !backdropUrl.trim().isEmpty() ? backdropUrl.trim() : null);
            System.out.println("Setting backdropUrl: " + movie.getBackdropUrl());
        }
        if (updates.containsKey("trailerUrl")) {
            movie.setTrailerUrl((String) updates.get("trailerUrl"));
        }
        if (updates.containsKey("videoUrl")) {
            movie.setVideoUrl((String) updates.get("videoUrl"));
        }
        if (updates.containsKey("duration")) {
            Object duration = updates.get("duration");
            if (duration instanceof Number) {
                movie.setDuration(((Number) duration).intValue());
            }
        }
        if (updates.containsKey("price")) {
            Object price = updates.get("price");
            if (price instanceof Number) {
                movie.setPrice(((Number) price).doubleValue());
            }
        }
        if (updates.containsKey("isVipOnly")) {
            movie.setIsVipOnly((Boolean) updates.get("isVipOnly"));
        }
        if (updates.containsKey("isActive")) {
            movie.setIsActive((Boolean) updates.get("isActive"));
        }
        if (updates.containsKey("type")) {
            try {
                String typeStr = (String) updates.get("type");
                movie.setType(MovieEntity.MovieType.valueOf(typeStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Ignore invalid type
            }
        }
        if (updates.containsKey("status")) {
            String statusStr = (String) updates.get("status");
            try {
                movie.setStatus(MovieEntity.MovieStatus.valueOf(statusStr.toUpperCase()));
                System.out.println("Setting status: " + movie.getStatus());
            } catch (IllegalArgumentException e) {
                // Giữ nguyên status hiện tại
            }
        }

        // Cập nhật categories nếu có categoryIds trong updates
        if (updates.containsKey("categoryIds")) {
            Object categoryIdsObj = updates.get("categoryIds");
            if (categoryIdsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> categoryIdsList = (List<Object>) categoryIdsObj;
                Set<CategoryEntity> categories = new HashSet<>();

                for (Object categoryIdObj : categoryIdsList) {
                    Long categoryId = null;
                    if (categoryIdObj instanceof Number) {
                        categoryId = ((Number) categoryIdObj).longValue();
                    } else if (categoryIdObj instanceof String) {
                        try {
                            categoryId = Long.parseLong((String) categoryIdObj);
                        } catch (NumberFormatException e) {
                            // Skip invalid category ID
                            continue;
                        }
                    }

                    if (categoryId != null) {
                        categoryRepository.findById(categoryId).ifPresent(categories::add);
                    }
                }

                movie.setCategories(categories);
                System.out.println("Setting categories: " + categories.size() + " categories");
            }
        }

        // Lưu vào database
        movie = movieRepository.save(movie);

        return mapToMovieResponse(movie);
    }

    /**
     * Xóa phim (soft delete - set isActive = false)
     */
    @Transactional
    public void deleteMovie(Long id) throws DataNotFoundException {
        MovieEntity movie = movieRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy phim với ID: " + id));

        movie.setIsActive(false);
        movieRepository.save(movie);
    }

    private MovieResponse mapToMovieResponse(MovieEntity movie) {
        return MovieResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .posterUrl(movie.getPosterUrl())
                .backdropUrl(movie.getBackdropUrl())
                .trailerUrl(movie.getTrailerUrl())
                .videoUrl(movie.getVideoUrl())
                .type(movie.getType().name())
                .status(movie.getStatus() != null ? movie.getStatus().name() : "UPCOMING")
                .duration(movie.getDuration())
                .releaseDate(movie.getReleaseDate())
                .rating(movie.getRating())
                .ratingCount(movie.getRatingCount())
                .viewCount(movie.getViewCount())
                .isVipOnly(movie.getIsVipOnly())
                .price(movie.getPrice())
                .isActive(movie.getIsActive())
                .categories(movie.getCategories().stream()
                        .map(c -> c.getName())
                        .collect(Collectors.toList()))
                .createdAt(movie.getCreatedAt())
                .updatedAt(movie.getUpdatedAt())
                .build();
    }
}


