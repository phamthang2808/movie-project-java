package com.example.thangcachep.movie_project_be.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.example.thangcachep.movie_project_be.models.request.MovieRequest;
import com.example.thangcachep.movie_project_be.models.responses.ApiResponse;
import com.example.thangcachep.movie_project_be.models.responses.MovieResponse;
import com.example.thangcachep.movie_project_be.services.impl.MovieService;
import com.example.thangcachep.movie_project_be.utils.MessageKeys;

import lombok.RequiredArgsConstructor;

/**
 * Admin Movie Controller
 * Quyền hạn: Admin có đầy đủ quyền (CRUD + approve/reject)
 * Sử dụng @PreAuthorize để kiểm tra quyền truy cập
 */
@RestController
@RequestMapping("/api/v1/admin/movies")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')") // Tất cả các method trong controller này yêu cầu role ADMIN
public class AdminMovieController {

    private final MovieService movieService;
    private final LocalizationUtils localizationUtils;

    /**
     * Lấy tất cả phim (bao gồm pending, rejected, deleted)
     * GET /api/v1/admin/movies
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<MovieResponse>>> getAllMovies(
            @RequestParam(required = false, defaultValue = "false") Boolean includeInactive
    ) {
        List<MovieResponse> movies = movieService.getAllMoviesWithoutPagination(includeInactive);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_GET_ALL_SUCCESS);
        ApiResponse<List<MovieResponse>> response = ApiResponse.success(message, movies);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách phim chờ duyệt (PENDING)
     * GET /api/v1/admin/movies/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<MovieResponse>>> getPendingMovies() {
        List<MovieResponse> movies = movieService.getPendingMovies();
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_GET_ALL_SUCCESS); // Dùng key chung
        ApiResponse<List<MovieResponse>> response = ApiResponse.success(message, movies);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy chi tiết phim
     * GET /api/v1/admin/movies/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MovieResponse>> getMovieById(@PathVariable Long id) {
        MovieResponse movie = movieService.getMovieById(id);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_GET_SUCCESS);
        ApiResponse<MovieResponse> response = ApiResponse.success(message, movie);
        return ResponseEntity.ok(response);
    }

    /**
     * Tạo phim mới (status: ACTIVE/AIRING ngay)
     * POST /api/v1/admin/movies
     */
    @PostMapping
    public ResponseEntity<ApiResponse<MovieResponse>> createMovie(@RequestBody MovieRequest request) {
        MovieResponse movie = movieService.createMovieForAdmin(request);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_CREATE_SUCCESS);
        ApiResponse<MovieResponse> response = ApiResponse.success(message, 201, movie);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Cập nhật phim (có thể sửa tất cả phim)
     * PUT /api/v1/admin/movies/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MovieResponse>> updateMovie(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates
    ) {
        MovieResponse movie = movieService.updateMovieForAdmin(id, updates);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_UPDATE_SUCCESS);
        ApiResponse<MovieResponse> response = ApiResponse.success(message, movie);
        return ResponseEntity.ok(response);
    }

    /**
     * Xóa phim (soft delete)
     * DELETE /api/v1/admin/movies/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_DELETE_SUCCESS);
        ApiResponse<Void> response = ApiResponse.success(message, null);
        return ResponseEntity.ok(response);
    }

    /**
     * Duyệt phim (chuyển từ PENDING sang ACTIVE/AIRING)
     * POST /api/v1/admin/movies/{id}/approve
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<MovieResponse>> approveMovie(
            @PathVariable Long id,
            @RequestParam(required = false) String status // AIRING, UPCOMING, ACTIVE
    ) {
        MovieResponse movie = movieService.approveMovie(id, status);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_APPROVE_SUCCESS);
        ApiResponse<MovieResponse> response = ApiResponse.success(message, movie);
        return ResponseEntity.ok(response);
    }

    /**
     * Từ chối phim (chuyển từ PENDING sang REJECTED)
     * POST /api/v1/admin/movies/{id}/reject
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<MovieResponse>> rejectMovie(@PathVariable Long id) {
        MovieResponse movie = movieService.rejectMovie(id);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_REJECT_SUCCESS);
        ApiResponse<MovieResponse> response = ApiResponse.success(message, movie);
        return ResponseEntity.ok(response);
    }
}

