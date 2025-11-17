package com.example.thangcachep.movie_project_be.controllers;

import java.util.List;
import java.util.Map;

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

import com.example.thangcachep.movie_project_be.exceptions.DataNotFoundException;
import com.example.thangcachep.movie_project_be.models.request.MovieRequest;
import com.example.thangcachep.movie_project_be.models.responses.MovieResponse;
import com.example.thangcachep.movie_project_be.services.impl.MovieService;

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

    /**
     * Lấy tất cả phim (bao gồm pending, rejected, deleted)
     * GET /api/v1/admin/movies
     */
    @GetMapping
    public ResponseEntity<List<MovieResponse>> getAllMovies(
            @RequestParam(required = false, defaultValue = "false") Boolean includeInactive
    ) {
        try {
            List<MovieResponse> movies = movieService.getAllMoviesWithoutPagination(includeInactive);
            return ResponseEntity.ok(movies);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Lấy danh sách phim chờ duyệt (PENDING)
     * GET /api/v1/admin/movies/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<List<MovieResponse>> getPendingMovies() {
        try {
            List<MovieResponse> movies = movieService.getPendingMovies();
            return ResponseEntity.ok(movies);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Lấy chi tiết phim
     * GET /api/v1/admin/movies/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<MovieResponse> getMovieById(@PathVariable Long id) {
        try {
            MovieResponse movie = movieService.getMovieById(id);
            return ResponseEntity.ok(movie);
        } catch (DataNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Tạo phim mới (status: ACTIVE/AIRING ngay)
     * POST /api/v1/admin/movies
     */
    @PostMapping
    public ResponseEntity<MovieResponse> createMovie(@RequestBody MovieRequest request) {
        try {
            MovieResponse movie = movieService.createMovieForAdmin(request);
            return ResponseEntity.ok(movie);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Cập nhật phim (có thể sửa tất cả phim)
     * PUT /api/v1/admin/movies/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<MovieResponse> updateMovie(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates
    ) {
        try {
            MovieResponse movie = movieService.updateMovieForAdmin(id, updates);
            return ResponseEntity.ok(movie);
        } catch (DataNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Xóa phim (soft delete)
     * DELETE /api/v1/admin/movies/{id}
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
     * Duyệt phim (chuyển từ PENDING sang ACTIVE/AIRING)
     * POST /api/v1/admin/movies/{id}/approve
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<MovieResponse> approveMovie(
            @PathVariable Long id,
            @RequestParam(required = false) String status // AIRING, UPCOMING, ACTIVE
    ) {
        try {
            MovieResponse movie = movieService.approveMovie(id, status);
            return ResponseEntity.ok(movie);
        } catch (DataNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            // Lỗi validation (ví dụ: phim không phải PENDING)
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Từ chối phim (chuyển từ PENDING sang REJECTED)
     * POST /api/v1/admin/movies/{id}/reject
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<MovieResponse> rejectMovie(@PathVariable Long id) {
        try {
            MovieResponse movie = movieService.rejectMovie(id);
            return ResponseEntity.ok(movie);
        } catch (DataNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            // Lỗi validation (ví dụ: phim không phải PENDING)
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

