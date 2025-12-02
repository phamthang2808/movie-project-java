package com.example.thangcachep.movie_project_be.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.thangcachep.movie_project_be.components.LocalizationUtils;
import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.exceptions.InvalidParamException;
import com.example.thangcachep.movie_project_be.exceptions.UnauthorizedException;
import com.example.thangcachep.movie_project_be.models.request.CommentRequest;
import com.example.thangcachep.movie_project_be.models.request.MovieRequest;
import com.example.thangcachep.movie_project_be.models.responses.ApiResponse;
import com.example.thangcachep.movie_project_be.models.responses.CommentResponse;
import com.example.thangcachep.movie_project_be.models.responses.EpisodeResponse;
import com.example.thangcachep.movie_project_be.models.responses.MovieResponse;
import com.example.thangcachep.movie_project_be.services.impl.CommentService;
import com.example.thangcachep.movie_project_be.services.impl.MovieService;
import com.example.thangcachep.movie_project_be.utils.MessageKeys;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MovieController {

    private final MovieService movieService;
    private final CommentService commentService;
    private final LocalizationUtils localizationUtils;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<MovieResponse>>> getAllMovies(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean includeInactive,
            Pageable pageable
    ) {
        Page<MovieResponse> movies = movieService.getAllMovies(type, categoryId, includeInactive, pageable);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_GET_ALL_SUCCESS);
        ApiResponse<Page<MovieResponse>> response = ApiResponse.success(message, movies);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy tất cả phim không phân trang (dùng cho admin)
     * Mặc định chỉ lấy phim active (isActive = true)
     * Truyền ?includeInactive=true để lấy cả phim đã xóa
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<MovieResponse>>> getAllMoviesWithoutPagination(
            @RequestParam(required = false, defaultValue = "false") Boolean includeInactive
    ) {
        List<MovieResponse> movies = movieService.getAllMoviesWithoutPagination(includeInactive);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_GET_ALL_SUCCESS);
        ApiResponse<List<MovieResponse>> response = ApiResponse.success(message, movies);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách comments của một phim
     * GET /api/v1/movies/{movieId}/comments
     * Đặt trước /{id} để tránh conflict routing
     */
    @GetMapping("/{movieId}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getMovieComments(@PathVariable Long movieId) {
        // Lấy user hiện tại (có thể null nếu chưa đăng nhập)
        UserEntity currentUser = getCurrentUser();
        Long userId = currentUser != null ? currentUser.getId() : null;

        List<CommentResponse> comments = commentService.getMovieComments(movieId, userId);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_GET_COMMENTS_SUCCESS);
        ApiResponse<List<CommentResponse>> response = ApiResponse.success(message, comments);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MovieResponse>> getMovieById(@PathVariable Long id) {
        // Lấy user hiện tại (có thể null nếu chưa đăng nhập)
        UserEntity currentUser = getCurrentUser();
        Long userId = currentUser != null ? currentUser.getId() : null;

        MovieResponse movie = movieService.getMovieById(id, userId);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_GET_SUCCESS);
        ApiResponse<MovieResponse> response = ApiResponse.success(message, movie);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách episodes của một phim bộ
     * GET /api/v1/movies/{movieId}/episodes
     */
    @GetMapping("/{movieId}/episodes")
    public ResponseEntity<ApiResponse<List<EpisodeResponse>>> getMovieEpisodes(@PathVariable Long movieId) {
        // Lấy user hiện tại (có thể null nếu chưa đăng nhập)
        UserEntity currentUser = getCurrentUser();
        Long userId = currentUser != null ? currentUser.getId() : null;

        List<EpisodeResponse> episodes = movieService.getEpisodesByMovieId(movieId, userId);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_GET_EPISODES_SUCCESS);
        ApiResponse<List<EpisodeResponse>> response = ApiResponse.success(message, episodes);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<MovieResponse>>> searchMovies(
            @RequestParam String q,
            Pageable pageable
    ) {
        Page<MovieResponse> movies = movieService.searchMovies(q, pageable);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_SEARCH_SUCCESS);
        ApiResponse<Page<MovieResponse>> response = ApiResponse.success(message, movies);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<List<MovieResponse>>> getTrendingMovies() {
        List<MovieResponse> movies = movieService.getTrendingMovies();
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_GET_TRENDING_SUCCESS);
        ApiResponse<List<MovieResponse>> response = ApiResponse.success(message, movies);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/top-week")
    public ResponseEntity<ApiResponse<List<MovieResponse>>> getTopMoviesWeek() {
        List<MovieResponse> movies = movieService.getTopMoviesWeek();
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_GET_TOP_WEEK_SUCCESS);
        ApiResponse<List<MovieResponse>> response = ApiResponse.success(message, movies);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<Page<MovieResponse>>> getMoviesByCategory(
            @PathVariable Long categoryId,
            Pageable pageable
    ) {
        Page<MovieResponse> movies = movieService.getMoviesByCategory(categoryId, pageable);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_GET_BY_CATEGORY_SUCCESS);
        ApiResponse<Page<MovieResponse>> response = ApiResponse.success(message, movies);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách phim đề xuất dựa trên phim hiện tại
     * GET /api/v1/movies/{movieId}/recommendations
     */
    @GetMapping("/{movieId}/recommendations")
    public ResponseEntity<ApiResponse<List<MovieResponse>>> getRecommendedMovies(@PathVariable Long movieId) {
        List<MovieResponse> movies = movieService.getRecommendedMovies(movieId);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_GET_RECOMMENDATIONS_SUCCESS);
        ApiResponse<List<MovieResponse>> response = ApiResponse.success(message, movies);
        return ResponseEntity.ok(response);
    }

    /**
     * Tạo phim mới
     * POST /api/v1/movies
     */
    @PostMapping
    public ResponseEntity<ApiResponse<MovieResponse>> createMovie(@RequestBody MovieRequest request) {
        MovieResponse movie = movieService.createMovie(request);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_CREATE_SUCCESS);
        ApiResponse<MovieResponse> response = ApiResponse.success(message, 201, movie);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Cập nhật thông tin phim (bao gồm videoUrl)
     * PUT /api/v1/movies/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MovieResponse>> updateMovie(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates
    ) {
        MovieResponse movie = movieService.updateMovie(id, updates);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_UPDATE_SUCCESS);
        ApiResponse<MovieResponse> response = ApiResponse.success(message, movie);
        return ResponseEntity.ok(response);
    }

    /**
     * Xóa phim (soft delete - set isActive = false)
     * DELETE /api/v1/movies/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_DELETE_SUCCESS);
        ApiResponse<Void> response = ApiResponse.success(message, null);
        return ResponseEntity.ok(response);
    }

    /**
     * Thêm comment cho một phim
     * POST /api/v1/movies/{movieId}/comments
     * Yêu cầu authentication (user phải đăng nhập)
     */
    @PostMapping("/{movieId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable Long movieId,
            @RequestBody @Valid CommentRequest request
    ) {
        // Lấy user hiện tại từ SecurityContext
        UserEntity user = getCurrentUser();

        if (user == null) {
            throw new UnauthorizedException("Bạn phải đăng nhập để bình luận");
        }

        CommentResponse comment = commentService.createComment(movieId, request, user);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.COMMENT_CREATE_SUCCESS);
        ApiResponse<CommentResponse> response = ApiResponse.success(message, comment);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy user hiện tại từ SecurityContext
     * @return UserEntity nếu đã đăng nhập, null nếu chưa đăng nhập
     */
    private UserEntity getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication.getPrincipal() == null) {
                return null;
            }

            // Kiểm tra xem principal có phải là UserEntity không
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserEntity) {
                return (UserEntity) principal;
            }

            // Nếu principal là String (anonymous user), return null
            if (principal instanceof String && principal.equals("anonymousUser")) {
                return null;
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Toggle like/unlike một comment
     * POST /api/v1/movies/{movieId}/comments/{commentId}/like
     * Yêu cầu authentication (user phải đăng nhập)
     * Like nếu chưa like, unlike nếu đã like
     */
    @PostMapping("/{movieId}/comments/{commentId}/like")
    public ResponseEntity<ApiResponse<CommentResponse>> toggleLikeComment(
            @PathVariable Long movieId,
            @PathVariable Long commentId
    ) {
        // Kiểm tra movie có tồn tại không (sẽ throw DataNotFoundException nếu không tìm thấy)
        movieService.getMovieById(movieId);

        // Lấy user hiện tại
        UserEntity currentUser = getCurrentUser();

        if (currentUser == null) {
            throw new UnauthorizedException("Bạn phải đăng nhập để like comment");
        }

        CommentResponse comment = commentService.toggleLikeComment(commentId, currentUser);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_LIKE_COMMENT_SUCCESS);
        ApiResponse<CommentResponse> response = ApiResponse.success(message, comment);
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // FAVORITES ENDPOINTS
    // ==========================================

    /**
     * Thêm phim vào danh sách yêu thích
     * POST /api/v1/movies/{movieId}/favorite
     * Yêu cầu authentication
     */
    @PostMapping("/{movieId}/favorite")
    public ResponseEntity<ApiResponse<Void>> addToFavorites(@PathVariable Long movieId) {
        UserEntity currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Bạn phải đăng nhập để thêm vào yêu thích");
        }

        movieService.addToFavorites(movieId, currentUser.getId());
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_ADD_FAVORITE_SUCCESS);
        ApiResponse<Void> response = ApiResponse.success(message, null);
        return ResponseEntity.ok(response);
    }

    /**
     * Xóa phim khỏi danh sách yêu thích
     * DELETE /api/v1/movies/{movieId}/favorite
     * Yêu cầu authentication
     */
    @DeleteMapping("/{movieId}/favorite")
    public ResponseEntity<ApiResponse<Void>> removeFromFavorites(@PathVariable Long movieId) {
        UserEntity currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Bạn phải đăng nhập để xóa khỏi yêu thích");
        }

        movieService.removeFromFavorites(movieId, currentUser.getId());
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_REMOVE_FAVORITE_SUCCESS);
        ApiResponse<Void> response = ApiResponse.success(message, null);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách phim yêu thích của user
     * GET /api/v1/movies/favorites
     * Yêu cầu authentication
     */
    @GetMapping("/favorites")
    public ResponseEntity<ApiResponse<List<MovieResponse>>> getFavoriteMovies() {
        UserEntity currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Bạn phải đăng nhập để xem danh sách yêu thích");
        }

        List<MovieResponse> favorites = movieService.getFavoriteMovies(currentUser.getId());
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_GET_FAVORITES_SUCCESS);
        ApiResponse<List<MovieResponse>> response = ApiResponse.success(message, favorites);
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // WATCHLIST ENDPOINTS
    // ==========================================

    /**
     * Thêm phim vào watchlist (danh sách xem sau)
     * POST /api/v1/movies/{movieId}/watchlist
     * Yêu cầu authentication
     */
    @PostMapping("/{movieId}/watchlist")
    public ResponseEntity<ApiResponse<Void>> addToWatchlist(@PathVariable Long movieId) {
        UserEntity currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Bạn phải đăng nhập để thêm vào danh sách");
        }

        movieService.addToWatchlist(movieId, currentUser.getId());
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_ADD_WATCHLIST_SUCCESS);
        ApiResponse<Void> response = ApiResponse.success(message, null);
        return ResponseEntity.ok(response);
    }

    /**
     * Xóa phim khỏi watchlist
     * DELETE /api/v1/movies/{movieId}/watchlist
     * Yêu cầu authentication
     */
    @DeleteMapping("/{movieId}/watchlist")
    public ResponseEntity<ApiResponse<Void>> removeFromWatchlist(@PathVariable Long movieId) {
        UserEntity currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Bạn phải đăng nhập để xóa khỏi danh sách");
        }

        movieService.removeFromWatchlist(movieId, currentUser.getId());
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_REMOVE_WATCHLIST_SUCCESS);
        ApiResponse<Void> response = ApiResponse.success(message, null);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách watchlist của user
     * GET /api/v1/movies/watchlist
     * Yêu cầu authentication
     */
    @GetMapping("/watchlist")
    public ResponseEntity<ApiResponse<List<MovieResponse>>> getWatchlist() {
        UserEntity currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Bạn phải đăng nhập để xem danh sách");
        }

        List<MovieResponse> watchlist = movieService.getWatchlist(currentUser.getId());
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_GET_WATCHLIST_SUCCESS);
        ApiResponse<List<MovieResponse>> response = ApiResponse.success(message, watchlist);
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // WATCH HISTORY ENDPOINTS
    // ==========================================

    /**
     * Lấy lịch sử xem phim của user (xem tiếp)
     * GET /api/v1/movies/history
     * Yêu cầu authentication
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getWatchHistory() {
        UserEntity currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Bạn phải đăng nhập để xem lịch sử");
        }

        List<Map<String, Object>> history = movieService.getWatchHistoryWithProgress(currentUser.getId());
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_GET_HISTORY_SUCCESS);
        ApiResponse<List<Map<String, Object>>> response = ApiResponse.success(message, history);
        return ResponseEntity.ok(response);
    }

    /**
     * Cập nhật tiến độ xem phim
     * POST /api/v1/movies/{movieId}/progress
     * Yêu cầu authentication
     * Body: { "progress": 3600 } (tính bằng giây)
     */
    @PostMapping("/{movieId}/progress")
    public ResponseEntity<ApiResponse<Void>> updateWatchProgress(
            @PathVariable Long movieId,
            @RequestBody Map<String, Object> request
    ) {
        UserEntity currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Bạn phải đăng nhập để cập nhật tiến độ");
        }

        Integer progress = null;
        if (request.containsKey("progress")) {
            Object progressObj = request.get("progress");
            if (progressObj instanceof Number) {
                progress = ((Number) progressObj).intValue();
            } else if (progressObj instanceof String) {
                progress = Integer.parseInt((String) progressObj);
            }
        }

        if (progress == null) {
            throw new InvalidParamException("Progress không hợp lệ");
        }

        Long episodeId = null;
        if (request.containsKey("episodeId")) {
            Object episodeIdObj = request.get("episodeId");
            if (episodeIdObj instanceof Number) {
                episodeId = ((Number) episodeIdObj).longValue();
            } else if (episodeIdObj instanceof String) {
                episodeId = Long.parseLong((String) episodeIdObj);
            }
        }

        movieService.updateWatchProgress(movieId, currentUser.getId(), progress, episodeId);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_UPDATE_PROGRESS_SUCCESS);
        ApiResponse<Void> response = ApiResponse.success(message, null);
        return ResponseEntity.ok(response);
    }
}


