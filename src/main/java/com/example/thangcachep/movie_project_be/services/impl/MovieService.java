package com.example.thangcachep.movie_project_be.services.impl;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import com.example.thangcachep.movie_project_be.entities.CategoryEntity;
import com.example.thangcachep.movie_project_be.entities.EpisodeEntity;
import com.example.thangcachep.movie_project_be.entities.FavoriteEntity;
import com.example.thangcachep.movie_project_be.entities.MovieEntity;
import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.entities.WatchHistoryEntity;
import com.example.thangcachep.movie_project_be.entities.WatchlistEntity;
import com.example.thangcachep.movie_project_be.exceptions.DataNotFoundException;
import com.example.thangcachep.movie_project_be.models.request.MovieRequest;
import com.example.thangcachep.movie_project_be.models.responses.EpisodeResponse;
import com.example.thangcachep.movie_project_be.models.responses.MovieResponse;
import com.example.thangcachep.movie_project_be.repositories.CategoryRepository;
import com.example.thangcachep.movie_project_be.repositories.EpisodeRepository;
import com.example.thangcachep.movie_project_be.repositories.FavoriteRepository;
import com.example.thangcachep.movie_project_be.repositories.MovieRepository;
import com.example.thangcachep.movie_project_be.repositories.UserRepository;
import com.example.thangcachep.movie_project_be.repositories.WatchHistoryRepository;
import com.example.thangcachep.movie_project_be.repositories.WatchlistRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final CategoryRepository categoryRepository;
    private final FavoriteRepository favoriteRepository;
    private final WatchlistRepository watchlistRepository;
    private final WatchHistoryRepository watchHistoryRepository;
    private final UserRepository userRepository;
    private final EpisodeRepository episodeRepository;

    public Page<MovieResponse> getAllMovies(String type, Long categoryId, Boolean includeInactive, Pageable pageable) {
        Page<MovieEntity> movies;

        if (categoryId != null) {
            // findByCategoryId đã filter isActive = true và không có PENDING/REJECTED
            movies = movieRepository.findByCategoryId(categoryId, pageable);
        } else if (type != null) {
            MovieEntity.MovieType movieType = MovieEntity.MovieType.valueOf(type.toUpperCase());
            // Nếu includeInactive = true, lấy cả phim inactive (nhưng vẫn exclude PENDING/REJECTED)
            if (includeInactive != null && includeInactive) {
                // Lấy tất cả nhưng filter PENDING/REJECTED ở code
                Page<MovieEntity> allMovies = movieRepository.findByType(movieType, pageable);
                // Filter và map
                List<MovieEntity> filteredMovies = allMovies.getContent().stream()
                        .filter(m -> m.getStatus() != MovieEntity.MovieStatus.PENDING &&
                                m.getStatus() != MovieEntity.MovieStatus.REJECTED)
                        .collect(Collectors.toList());
                // Tạo Page mới từ filtered list
                movies = new org.springframework.data.domain.PageImpl<>(filteredMovies, pageable, filteredMovies.size());
            } else {
                // Mặc định chỉ lấy phim active và không phải PENDING/REJECTED (phim đã được duyệt)
                movies = movieRepository.findByTypeAndIsActiveTrue(movieType, pageable);
            }
        } else {
            // Nếu includeInactive = true, lấy tất cả (kể cả inactive nhưng exclude PENDING/REJECTED)
            if (includeInactive != null && includeInactive) {
                Page<MovieEntity> allMovies = movieRepository.findAll(pageable);
                // Filter và map
                List<MovieEntity> filteredMovies = allMovies.getContent().stream()
                        .filter(m -> m.getStatus() != MovieEntity.MovieStatus.PENDING &&
                                m.getStatus() != MovieEntity.MovieStatus.REJECTED)
                        .collect(Collectors.toList());
                // Tạo Page mới từ filtered list
                movies = new org.springframework.data.domain.PageImpl<>(filteredMovies, pageable, filteredMovies.size());
            } else {
                // Chỉ lấy phim active và không phải PENDING/REJECTED
                movies = movieRepository.findByIsActiveTrueAndApproved(pageable);
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
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy phim với ID: " + id));
        return mapToMovieResponse(movie);
    }

    /**
     * Lấy danh sách episodes của một phim bộ
     * Chỉ lấy các episode active, sắp xếp theo episodeNumber
     */
    public List<EpisodeResponse> getEpisodesByMovieId(Long movieId) {
        // Kiểm tra phim có tồn tại không
        movieRepository.findById(movieId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy phim với ID: " + movieId));

        // Lấy danh sách episodes, chỉ lấy active, sắp xếp theo episodeNumber
        List<EpisodeEntity> episodes = episodeRepository.findByMovieIdOrderByEpisodeNumberAsc(movieId);

        // Filter chỉ lấy active episodes
        return episodes.stream()
                .filter(episode -> episode.getIsActive() != null && episode.getIsActive())
                .map(this::mapToEpisodeResponse)
                .collect(Collectors.toList());
    }

    /**
     * Map EpisodeEntity sang EpisodeResponse
     */
    private EpisodeResponse mapToEpisodeResponse(EpisodeEntity episode) {
        return EpisodeResponse.builder()
                .id(episode.getId())
                .movieId(episode.getMovie().getId())
                .episodeNumber(episode.getEpisodeNumber())
                .title(episode.getTitle())
                .description(episode.getDescription())
                .videoUrl(episode.getVideoUrl())
                .duration(episode.getDuration())
                .isVipOnly(episode.getIsVipOnly())
                .isActive(episode.getIsActive())
                .build();
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
     * Lấy danh sách phim đề xuất dựa trên phim hiện tại
     * Logic: Lấy phim cùng thể loại, cùng type, có rating cao, loại trừ phim hiện tại
     */
    public List<MovieResponse> getRecommendedMovies(Long movieId) {
        // Lấy phim hiện tại
        MovieEntity currentMovie = movieRepository.findByIdWithCategories(movieId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy phim với ID: " + movieId));

        // Lấy các category IDs của phim hiện tại
        Set<Long> categoryIds = currentMovie.getCategories().stream()
                .map(CategoryEntity::getId)
                .collect(Collectors.toSet());

        // Nếu không có category, trả về phim trending hoặc top rated
        if (categoryIds.isEmpty()) {
            List<MovieEntity> topMovies = movieRepository.findTopRatedMovies(PageRequest.of(0, 10));
            return topMovies.stream()
                    .filter(m -> !m.getId().equals(movieId))
                    .limit(5)
                    .map(this::mapToMovieResponse)
                    .collect(Collectors.toList());
        }

        // Tìm phim cùng category, cùng type, loại trừ phim hiện tại
        List<MovieEntity> recommendedMovies = new java.util.ArrayList<>();

        // Lấy tất cả phim active và approved
        List<MovieEntity> allActiveMovies = movieRepository.findByIsActiveTrueWithCategories();

        for (MovieEntity movie : allActiveMovies) {
            // Loại trừ phim hiện tại
            if (movie.getId().equals(movieId)) {
                continue;
            }

            // Loại trừ phim PENDING/REJECTED
            if (movie.getStatus() == MovieEntity.MovieStatus.PENDING ||
                    movie.getStatus() == MovieEntity.MovieStatus.REJECTED) {
                continue;
            }

            // Kiểm tra có cùng category không
            boolean hasCommonCategory = movie.getCategories().stream()
                    .anyMatch(cat -> categoryIds.contains(cat.getId()));

            // Kiểm tra cùng type (nếu có)
            boolean sameType = currentMovie.getType() == null ||
                    movie.getType() == null ||
                    movie.getType().equals(currentMovie.getType());

            // Nếu có cùng category hoặc cùng type, thêm vào danh sách
            if (hasCommonCategory || sameType) {
                recommendedMovies.add(movie);
            }
        }

        // Sắp xếp theo rating và viewCount, sau đó giới hạn số lượng
        List<MovieEntity> sortedMovies = recommendedMovies.stream()
                .sorted((m1, m2) -> {
                    // Ưu tiên rating trước
                    int ratingCompare = Double.compare(
                            m2.getRating() != null ? m2.getRating() : 0.0,
                            m1.getRating() != null ? m1.getRating() : 0.0
                    );
                    if (ratingCompare != 0) {
                        return ratingCompare;
                    }
                    // Sau đó ưu tiên viewCount
                    return Long.compare(
                            m2.getViewCount() != null ? m2.getViewCount() : 0L,
                            m1.getViewCount() != null ? m1.getViewCount() : 0L
                    );
                })
                .limit(10) // Giới hạn 10 phim
                .collect(Collectors.toList());

        // Nếu không đủ phim, bổ sung thêm phim top rated
        if (sortedMovies.size() < 5) {
            List<MovieEntity> topRated = movieRepository.findTopRatedMovies(PageRequest.of(0, 10));
            for (MovieEntity movie : topRated) {
                if (sortedMovies.size() >= 10) {
                    break;
                }
                if (!movie.getId().equals(movieId) &&
                        !sortedMovies.stream().anyMatch(m -> m.getId().equals(movie.getId()))) {
                    sortedMovies.add(movie);
                }
            }
        }

        return sortedMovies.stream()
                .map(this::mapToMovieResponse)
                .collect(Collectors.toList());
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
     * Xóa phim khỏi database (hard delete)
     * Khác với rejectMovie (soft delete - chỉ set isActive = false)
     * Hành động này không thể hoàn tác - phim sẽ bị xóa vĩnh viễn
     */
    @Transactional
    public void deleteMovie(Long id) throws DataNotFoundException {
        MovieEntity movie = movieRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy phim với ID: " + id));

        // Hard delete: Xóa phim khỏi database hoàn toàn
        movieRepository.delete(movie);
    }

    private MovieResponse mapToMovieResponse(MovieEntity movie) {
        MovieResponse.MovieResponseBuilder builder = MovieResponse.builder()
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
                .updatedAt(movie.getUpdatedAt());

        // Map createdBy info
        if (movie.getCreatedBy() != null) {
            builder.createdById(movie.getCreatedBy().getId())
                    .createdByName(movie.getCreatedBy().getName() != null
                            ? movie.getCreatedBy().getName()
                            : movie.getCreatedBy().getEmail());
        }

        // Map approvedBy info
        if (movie.getApprovedBy() != null) {
            builder.approvedById(movie.getApprovedBy().getId())
                    .approvedByName(movie.getApprovedBy().getName() != null
                            ? movie.getApprovedBy().getName()
                            : movie.getApprovedBy().getEmail());
        }

        // Map approvedAt
        if (movie.getApprovedAt() != null) {
            builder.approvedAt(movie.getApprovedAt());
        }

        return builder.build();
    }

    // ==========================================
    // FAVORITES METHODS
    // ==========================================

    /**
     * Thêm phim vào danh sách yêu thích
     */
    @Transactional
    public void addToFavorites(Long movieId, Long userId) {
        // Kiểm tra phim có tồn tại không
        MovieEntity movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy phim với ID: " + movieId));

        // Kiểm tra user có tồn tại không
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy user với ID: " + userId));

        // Kiểm tra đã có trong favorites chưa
        if (favoriteRepository.existsByMovieIdAndUserId(movieId, userId)) {
            throw new IllegalArgumentException("Phim đã có trong danh sách yêu thích");
        }

        // Tạo favorite entity
        FavoriteEntity favorite = FavoriteEntity.builder()
                .movie(movie)
                .user(user)
                .build();

        favoriteRepository.save(favorite);
    }

    /**
     * Xóa phim khỏi danh sách yêu thích
     */
    @Transactional
    public void removeFromFavorites(Long movieId, Long userId) {
        FavoriteEntity favorite = favoriteRepository.findByMovieIdAndUserId(movieId, userId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy phim trong danh sách yêu thích"));

        favoriteRepository.delete(favorite);
    }

    /**
     * Lấy danh sách phim yêu thích của user
     */
    @Transactional(readOnly = true)
    public List<MovieResponse> getFavoriteMovies(Long userId) {
        // Lấy movie IDs trực tiếp từ database (tối ưu, tránh LazyInitializationException)
        List<Long> movieIds = favoriteRepository.findMovieIdsByUserId(userId);

        if (movieIds.isEmpty()) {
            return List.of();
        }

        // Load movies với categories (tối ưu với JOIN FETCH)
        List<MovieEntity> movies = movieRepository.findByIdsWithCategories(movieIds);

        return movies.stream()
                .map(this::mapToMovieResponse)
                .collect(Collectors.toList());
    }

    /**
     * Kiểm tra phim có trong danh sách yêu thích không
     */
    public boolean isFavorite(Long movieId, Long userId) {
        return favoriteRepository.existsByMovieIdAndUserId(movieId, userId);
    }

    // ==========================================
    // WATCHLIST METHODS
    // ==========================================

    /**
     * Thêm phim vào watchlist (danh sách xem sau)
     */
    @Transactional
    public void addToWatchlist(Long movieId, Long userId) {
        // Kiểm tra phim có tồn tại không
        MovieEntity movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy phim với ID: " + movieId));

        // Kiểm tra user có tồn tại không
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy user với ID: " + userId));

        // Kiểm tra đã có trong watchlist chưa
        if (watchlistRepository.existsByMovieIdAndUserId(movieId, userId)) {
            throw new IllegalArgumentException("Phim đã có trong danh sách xem");
        }

        // Tạo watchlist entity
        WatchlistEntity watchlist = WatchlistEntity.builder()
                .movie(movie)
                .user(user)
                .build();

        watchlistRepository.save(watchlist);
    }

    /**
     * Xóa phim khỏi watchlist
     */
    @Transactional
    public void removeFromWatchlist(Long movieId, Long userId) {
        WatchlistEntity watchlist = watchlistRepository.findByMovieIdAndUserId(movieId, userId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy phim trong danh sách xem"));

        watchlistRepository.delete(watchlist);
    }

    /**
     * Lấy danh sách watchlist của user
     */
    @Transactional(readOnly = true)
    public List<MovieResponse> getWatchlist(Long userId) {
        // Lấy movie IDs trực tiếp từ database (tối ưu, tránh LazyInitializationException)
        List<Long> movieIds = watchlistRepository.findMovieIdsByUserId(userId);

        if (movieIds.isEmpty()) {
            return List.of();
        }

        // Load movies với categories (tối ưu với JOIN FETCH)
        List<MovieEntity> movies = movieRepository.findByIdsWithCategories(movieIds);

        return movies.stream()
                .map(this::mapToMovieResponse)
                .collect(Collectors.toList());
    }

    /**
     * Kiểm tra phim có trong watchlist không
     */
    public boolean isInWatchlist(Long movieId, Long userId) {
        return watchlistRepository.existsByMovieIdAndUserId(movieId, userId);
    }

    // ==========================================
    // WATCH HISTORY METHODS
    // ==========================================

    /**
     * Lấy lịch sử xem phim của user (xem tiếp) - trả về Map chứa cả movie và watch progress
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getWatchHistoryWithProgress(Long userId) {
        // Lấy watch histories với movie và categories đã được fetch (tối ưu)
        List<WatchHistoryEntity> watchHistories = watchHistoryRepository.findByUserIdWithMovieAndCategories(userId);

        if (watchHistories.isEmpty()) {
            return List.of();
        }

        // Lấy movies và giữ nguyên thứ tự (theo updatedAt DESC), lấy watch history mới nhất cho mỗi phim
        // Sử dụng LinkedHashMap để giữ thứ tự và Map để lưu watch history info
        Map<Long, MovieEntity> movieMap = new LinkedHashMap<>();
        Map<Long, WatchHistoryEntity> watchHistoryMap = new LinkedHashMap<>();

        for (WatchHistoryEntity watchHistory : watchHistories) {
            MovieEntity movie = watchHistory.getMovie();
            if (movie != null) {
                Long movieId = movie.getId();
                // Chỉ lấy watch history mới nhất cho mỗi phim (theo updatedAt DESC)
                if (!movieMap.containsKey(movieId) ||
                        (watchHistoryMap.containsKey(movieId) &&
                                watchHistory.getUpdatedAt().isAfter(watchHistoryMap.get(movieId).getUpdatedAt()))) {
                    movieMap.put(movieId, movie);
                    watchHistoryMap.put(movieId, watchHistory);
                }
            }
        }

        // Map sang Map chứa cả movie response và watch progress info
        return movieMap.entrySet().stream()
                .map(entry -> {
                    MovieEntity movie = entry.getValue();
                    WatchHistoryEntity watchHistory = watchHistoryMap.get(entry.getKey());
                    MovieResponse movieResponse = mapToMovieResponse(movie);

                    // Tạo Map chứa cả movie response và watch progress
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("id", movieResponse.getId());
                    result.put("movieId", movieResponse.getId());
                    result.put("title", movieResponse.getTitle());
                    result.put("originalTitle", movieResponse.getTitle()); // Có thể thêm field originalTitle sau
                    result.put("posterUrl", movieResponse.getPosterUrl());
                    result.put("backdropUrl", movieResponse.getBackdropUrl());
                    result.put("description", movieResponse.getDescription());
                    result.put("type", movieResponse.getType());
                    result.put("duration", movieResponse.getDuration());
                    result.put("releaseDate", movieResponse.getReleaseDate());
                    result.put("rating", movieResponse.getRating());
                    result.put("categories", movieResponse.getCategories());

                    // Thêm thông tin watch progress
                    result.put("watchProgress", watchHistory.getWatchProgress() != null ? watchHistory.getWatchProgress() : 0);
                    result.put("totalDuration", watchHistory.getTotalDuration() != null ? watchHistory.getTotalDuration() : (movieResponse.getDuration() != null ? movieResponse.getDuration() * 60 : 0));
                    if (watchHistory.getEpisode() != null) {
                        result.put("currentEpisodeId", watchHistory.getEpisode().getId());
                        result.put("currentEpisode", watchHistory.getEpisode().getEpisodeNumber());
                    }
                    result.put("updatedAt", watchHistory.getUpdatedAt());

                    return result;
                })
                .collect(Collectors.toList());
    }


    /**
     * Cập nhật tiến độ xem phim
     */
    @Transactional
    public void updateWatchProgress(Long movieId, Long userId, Integer progress, Long episodeId) {
        // Kiểm tra phim có tồn tại không
        MovieEntity movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy phim với ID: " + movieId));

        // Kiểm tra user có tồn tại không
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy user với ID: " + userId));

        // Lấy tổng thời lượng phim (tính bằng giây)
        Integer totalDuration = movie.getDuration() != null ? movie.getDuration() * 60 : 0;

        // Tìm watch history hiện tại
        WatchHistoryEntity watchHistory;
        if (episodeId != null) {
            // Nếu là series, tìm theo episode
            watchHistory = watchHistoryRepository.findByMovieIdAndUserIdAndEpisodeId(movieId, userId, episodeId)
                    .orElse(null);
        } else {
            // Nếu là phim lẻ, tìm theo movie
            watchHistory = watchHistoryRepository.findByMovieIdAndUserId(movieId, userId)
                    .orElse(null);
        }

        if (watchHistory == null) {
            // Tạo mới watch history
            watchHistory = WatchHistoryEntity.builder()
                    .movie(movie)
                    .user(user)
                    .watchProgress(progress)
                    .totalDuration(totalDuration)
                    .build();

            // Nếu là series, set episode
            if (episodeId != null) {
                episodeRepository.findById(episodeId).ifPresent(watchHistory::setEpisode);
            }
        } else {
            // Cập nhật progress
            watchHistory.setWatchProgress(progress);
            watchHistory.setTotalDuration(totalDuration);
        }

        watchHistoryRepository.save(watchHistory);
    }

    /**
     * Lấy thông tin watch history của một phim
     */
    public WatchHistoryEntity getWatchHistoryByMovie(Long movieId, Long userId, Long episodeId) {
        if (episodeId != null) {
            return watchHistoryRepository.findByMovieIdAndUserIdAndEpisodeId(movieId, userId, episodeId)
                    .orElse(null);
        } else {
            return watchHistoryRepository.findByMovieIdAndUserId(movieId, userId)
                    .orElse(null);
        }
    }

    // ==========================================
    // STAFF & ADMIN PERMISSION METHODS
    // ==========================================

    /**
     * Lấy user hiện tại từ SecurityContext
     */
    private UserEntity getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication.getPrincipal() == null) {
                return null;
            }

            Object principal = authentication.getPrincipal();
            if (principal instanceof UserEntity) {
                return (UserEntity) principal;
            }

            if (principal instanceof String && principal.equals("anonymousUser")) {
                return null;
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Tạo phim mới cho Staff (status: PENDING)
     */
    @Transactional
    public MovieResponse createMovieForStaff(MovieRequest request) {
        UserEntity currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("User chưa đăng nhập");
        }

        MovieEntity movie = new MovieEntity();
        setMovieBasicFields(movie, request);

        // Staff tạo phim → status PENDING
        movie.setStatus(MovieEntity.MovieStatus.PENDING);
        movie.setCreatedBy(currentUser);
        movie.setIsActive(true);

        movie = movieRepository.save(movie);
        return mapToMovieResponse(movie);
    }

    /**
     * Tạo phim mới cho Admin (status: ACTIVE/AIRING tùy chọn)
     */
    @Transactional
    public MovieResponse createMovieForAdmin(MovieRequest request) {
        UserEntity currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("User chưa đăng nhập");
        }

        MovieEntity movie = new MovieEntity();
        setMovieBasicFields(movie, request);

        // Admin tạo phim → status ACTIVE/AIRING ngay (không cần duyệt)
        // Nếu không có status trong request, mặc định là AIRING
        if (request.getStatus() == null || request.getStatus().isEmpty()) {
            movie.setStatus(MovieEntity.MovieStatus.AIRING);
        } else {
            try {
                MovieEntity.MovieStatus status = MovieEntity.MovieStatus.valueOf(request.getStatus().toUpperCase());
                // Admin không thể tạo phim với status PENDING
                if (status == MovieEntity.MovieStatus.PENDING) {
                    movie.setStatus(MovieEntity.MovieStatus.AIRING);
                } else {
                    movie.setStatus(status);
                }
            } catch (IllegalArgumentException e) {
                movie.setStatus(MovieEntity.MovieStatus.AIRING);
            }
        }

        movie.setCreatedBy(currentUser);
        // Admin tạo phim → tự động approve
        movie.setApprovedBy(currentUser);
        movie.setApprovedAt(LocalDateTime.now());
        movie.setIsActive(true);

        movie = movieRepository.save(movie);
        return mapToMovieResponse(movie);
    }

    /**
     * Helper method để set các field cơ bản của phim
     */
    private void setMovieBasicFields(MovieEntity movie, MovieRequest request) {
        movie.setTitle(request.getTitle());
        movie.setDescription(request.getDescription());
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
    }

    /**
     * Cập nhật phim cho Staff (chỉ cho phép sửa phim PENDING hoặc phim do Staff đó tạo)
     */
    @Transactional
    public MovieResponse updateMovieForStaff(Long id, Map<String, Object> updates) throws DataNotFoundException {
        UserEntity currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("User chưa đăng nhập");
        }

        MovieEntity movie = movieRepository.findByIdWithCategories(id)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy phim với ID: " + id));

        // Staff chỉ có thể sửa phim PENDING hoặc phim do chính mình tạo
        if (movie.getStatus() != MovieEntity.MovieStatus.PENDING) {
            // Nếu không phải PENDING, kiểm tra xem có phải phim do Staff đó tạo không
            if (movie.getCreatedBy() == null || !movie.getCreatedBy().getId().equals(currentUser.getId())) {
                throw new RuntimeException("Bạn không có quyền sửa phim này. Chỉ có thể sửa phim đang chờ duyệt hoặc phim do bạn tạo.");
            }
        }

        // Staff không thể thay đổi status thành ACTIVE/AIRING/COMPLETED
        if (updates.containsKey("status")) {
            String statusStr = (String) updates.get("status");
            try {
                MovieEntity.MovieStatus newStatus = MovieEntity.MovieStatus.valueOf(statusStr.toUpperCase());
                // Staff chỉ có thể set status là PENDING hoặc REJECTED (nếu muốn hủy)
                if (newStatus != MovieEntity.MovieStatus.PENDING && newStatus != MovieEntity.MovieStatus.REJECTED) {
                    throw new RuntimeException("Staff không thể đặt status thành " + newStatus + ". Vui lòng để Admin duyệt phim.");
                }
            } catch (IllegalArgumentException e) {
                // Ignore invalid status
            }
        }

        // Cập nhật các field khác
        updateMovieFields(movie, updates);

        movie = movieRepository.save(movie);
        return mapToMovieResponse(movie);
    }

    /**
     * Cập nhật phim cho Admin (có thể sửa tất cả phim)
     */
    @Transactional
    public MovieResponse updateMovieForAdmin(Long id, Map<String, Object> updates) throws DataNotFoundException {
        MovieEntity movie = movieRepository.findByIdWithCategories(id)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy phim với ID: " + id));

        // Admin có thể sửa tất cả phim
        updateMovieFields(movie, updates);

        movie = movieRepository.save(movie);
        return mapToMovieResponse(movie);
    }

    /**
     * Helper method để cập nhật các field của phim
     */
    private void updateMovieFields(MovieEntity movie, Map<String, Object> updates) {
        if (updates.containsKey("title")) {
            movie.setTitle((String) updates.get("title"));
        }
        if (updates.containsKey("description")) {
            movie.setDescription((String) updates.get("description"));
        }
        if (updates.containsKey("posterUrl")) {
            String posterUrl = (String) updates.get("posterUrl");
            movie.setPosterUrl(posterUrl != null && !posterUrl.trim().isEmpty() ? posterUrl.trim() : null);
        }
        if (updates.containsKey("backdropUrl")) {
            String backdropUrl = (String) updates.get("backdropUrl");
            movie.setBackdropUrl(backdropUrl != null && !backdropUrl.trim().isEmpty() ? backdropUrl.trim() : null);
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
            } catch (IllegalArgumentException e) {
                // Ignore invalid status
            }
        }

        // Cập nhật categories
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
                            continue;
                        }
                    }

                    if (categoryId != null) {
                        categoryRepository.findById(categoryId).ifPresent(categories::add);
                    }
                }

                movie.setCategories(categories);
            }
        }
    }

    /**
     * Duyệt phim (chuyển từ PENDING sang ACTIVE/AIRING)
     */
    @Transactional
    public MovieResponse approveMovie(Long id, String targetStatus) throws DataNotFoundException {
        UserEntity currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("User chưa đăng nhập");
        }

        MovieEntity movie = movieRepository.findByIdWithCategories(id)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy phim với ID: " + id));

        // Chỉ có thể duyệt phim PENDING
        if (movie.getStatus() != MovieEntity.MovieStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể duyệt phim đang chờ duyệt (PENDING)");
        }

        // Set status mới (mặc định là AIRING)
        MovieEntity.MovieStatus newStatus = MovieEntity.MovieStatus.AIRING;
        if (targetStatus != null && !targetStatus.isEmpty()) {
            try {
                newStatus = MovieEntity.MovieStatus.valueOf(targetStatus.toUpperCase());
                // Chỉ cho phép chuyển sang ACTIVE/AIRING/UPCOMING
                if (newStatus == MovieEntity.MovieStatus.PENDING || newStatus == MovieEntity.MovieStatus.REJECTED) {
                    throw new RuntimeException("Không thể duyệt phim với status " + newStatus);
                }
            } catch (IllegalArgumentException e) {
                newStatus = MovieEntity.MovieStatus.AIRING;
            }
        }

        movie.setStatus(newStatus);
        movie.setApprovedBy(currentUser);
        movie.setApprovedAt(LocalDateTime.now());
        movie.setIsActive(true);

        movie = movieRepository.save(movie);
        return mapToMovieResponse(movie);
    }

    /**
     * Từ chối phim (chuyển từ PENDING sang REJECTED)
     */
    @Transactional
    public MovieResponse rejectMovie(Long id) throws DataNotFoundException {
        UserEntity currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("User chưa đăng nhập");
        }

        MovieEntity movie = movieRepository.findByIdWithCategories(id)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy phim với ID: " + id));

        // Chỉ có thể từ chối phim PENDING
        if (movie.getStatus() != MovieEntity.MovieStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể từ chối phim đang chờ duyệt (PENDING)");
        }

        movie.setStatus(MovieEntity.MovieStatus.REJECTED);
        movie.setApprovedBy(currentUser);
        movie.setApprovedAt(LocalDateTime.now());
        movie.setIsActive(false); // Rejected phim không hiển thị

        movie = movieRepository.save(movie);
        return mapToMovieResponse(movie);
    }

    /**
     * Lấy danh sách phim chờ duyệt (PENDING)
     */
    public List<MovieResponse> getPendingMovies() {
        List<MovieEntity> movies = movieRepository.findByStatusWithCategories(MovieEntity.MovieStatus.PENDING);
        return movies.stream()
                .map(this::mapToMovieResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách phim do Staff tạo
     */
    public List<MovieResponse> getMoviesByStaff(Long staffId) {
        List<MovieEntity> movies = movieRepository.findByCreatedByIdWithCategories(staffId);
        return movies.stream()
                .map(this::mapToMovieResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách phim do Staff hiện tại tạo
     */
    public List<MovieResponse> getMoviesByCurrentStaff() {
        UserEntity currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("User chưa đăng nhập");
        }
        return getMoviesByStaff(currentUser.getId());
    }
}


