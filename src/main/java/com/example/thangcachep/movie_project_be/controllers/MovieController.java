package com.example.thangcachep.movie_project_be.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.exceptions.DataNotFoundException;
import com.example.thangcachep.movie_project_be.models.request.CommentRequest;
import com.example.thangcachep.movie_project_be.models.request.MovieRequest;
import com.example.thangcachep.movie_project_be.models.responses.CommentResponse;
import com.example.thangcachep.movie_project_be.models.responses.MovieResponse;
import com.example.thangcachep.movie_project_be.services.impl.CommentService;
import com.example.thangcachep.movie_project_be.services.impl.MovieService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MovieController {

    private final MovieService movieService;
    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<Page<MovieResponse>> getAllMovies(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean includeInactive,
            Pageable pageable
    ) {
        Page<MovieResponse> movies = movieService.getAllMovies(type, categoryId, includeInactive, pageable);
        return ResponseEntity.ok(movies);
    }

    /**
     * Lấy tất cả phim không phân trang (dùng cho admin)
     * Mặc định chỉ lấy phim active (isActive = true)
     * Truyền ?includeInactive=true để lấy cả phim đã xóa
     */
    @GetMapping("/all")
    public ResponseEntity<List<MovieResponse>> getAllMoviesWithoutPagination(
            @RequestParam(required = false, defaultValue = "false") Boolean includeInactive
    ) {
        List<MovieResponse> movies = movieService.getAllMoviesWithoutPagination(includeInactive);
        return ResponseEntity.ok(movies);
    }

    /**
     * Lấy danh sách comments của một phim
     * GET /api/v1/movies/{movieId}/comments
     * Đặt trước /{id} để tránh conflict routing
     */
    @GetMapping("/{movieId}/comments")
    public ResponseEntity<List<CommentResponse>> getMovieComments(@PathVariable Long movieId) {
        try {
            // Lấy user hiện tại (có thể null nếu chưa đăng nhập)
            UserEntity currentUser = getCurrentUser();
            Long userId = currentUser != null ? currentUser.getId() : null;

            List<CommentResponse> comments = commentService.getMovieComments(movieId, userId);
            return ResponseEntity.ok(comments);
        } catch (DataNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieResponse> getMovieById(@PathVariable Long id) {
        MovieResponse movie = movieService.getMovieById(id);
        return ResponseEntity.ok(movie);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<MovieResponse>> searchMovies(
            @RequestParam String q,
            Pageable pageable
    ) {
        Page<MovieResponse> movies = movieService.searchMovies(q, pageable);
        return ResponseEntity.ok(movies);
    }

    @GetMapping("/trending")
    public ResponseEntity<List<MovieResponse>> getTrendingMovies() {
        List<MovieResponse> movies = movieService.getTrendingMovies();
        return ResponseEntity.ok(movies);
    }

    @GetMapping("/top-week")
    public ResponseEntity<List<MovieResponse>> getTopMoviesWeek() {
        List<MovieResponse> movies = movieService.getTopMoviesWeek();
        return ResponseEntity.ok(movies);
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<MovieResponse>> getMoviesByCategory(
            @PathVariable Long categoryId,
            Pageable pageable
    ) {
        Page<MovieResponse> movies = movieService.getMoviesByCategory(categoryId, pageable);
        return ResponseEntity.ok(movies);
    }

    /**
     * Tạo phim mới
     * POST /api/v1/movies
     */
    @PostMapping
    public ResponseEntity<MovieResponse> createMovie(@RequestBody MovieRequest request) {
        try {
            MovieResponse movie = movieService.createMovie(request);
            return ResponseEntity.ok(movie);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Cập nhật thông tin phim (bao gồm videoUrl)
     * PUT /api/v1/movies/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<MovieResponse> updateMovie(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates
    ) {
        try {
            MovieResponse movie = movieService.updateMovie(id, updates);
            return ResponseEntity.ok(movie);
        } catch (DataNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Xóa phim (soft delete - set isActive = false)
     * DELETE /api/v1/movies/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
        try {
            movieService.deleteMovie(id);
            return ResponseEntity.ok().build();
        } catch (DataNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Thêm comment cho một phim
     * POST /api/v1/movies/{movieId}/comments
     * Yêu cầu authentication (user phải đăng nhập)
     */
    @PostMapping("/{movieId}/comments")
    public ResponseEntity<?> addComment(
            @PathVariable Long movieId,
            @RequestBody @Valid CommentRequest request
    ) {
        try {
            // Lấy user hiện tại từ SecurityContext
            UserEntity user = getCurrentUser();

            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized", "message", "Bạn phải đăng nhập để bình luận"));
            }

            CommentResponse comment = commentService.createComment(movieId, request, user);
            return ResponseEntity.ok(comment);
        } catch (DataNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", "Not Found", "message", e.getMessage()));
        } catch (ClassCastException | NullPointerException e) {
            // Lỗi khi không có authentication
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized", "message", "Bạn phải đăng nhập để bình luận"));
        } catch (Exception e) {
            e.printStackTrace(); // Log error để debug
            return ResponseEntity.status(400).body(Map.of("error", "Bad Request", "message", e.getMessage()));
        }
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
    public ResponseEntity<?> toggleLikeComment(
            @PathVariable Long movieId,
            @PathVariable Long commentId
    ) {
        try {
            // Kiểm tra movie có tồn tại không
            try {
                movieService.getMovieById(movieId);
            } catch (DataNotFoundException e) {
                return ResponseEntity.status(404).body(Map.of("error", "Not Found", "message", "Không tìm thấy phim"));
            }

            // Lấy user hiện tại (phải có, không thể null)
            UserEntity currentUser = getCurrentUser();

            // Kiểm tra user đã đăng nhập chưa
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized", "message", "Bạn phải đăng nhập để like comment"));
            }

            CommentResponse comment = commentService.toggleLikeComment(commentId, currentUser);
            return ResponseEntity.ok(comment);
        } catch (DataNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", "Not Found", "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            // Lỗi validation (ví dụ: chưa đăng nhập)
            return ResponseEntity.status(400).body(Map.of("error", "Bad Request", "message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(400).body(Map.of("error", "Bad Request", "message", e.getMessage()));
        }
    }
}


