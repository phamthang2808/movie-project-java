package com.example.thangcachep.movie_project_be.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.thangcachep.movie_project_be.exceptions.DataNotFoundException;
import com.example.thangcachep.movie_project_be.models.request.MovieRequest;
import com.example.thangcachep.movie_project_be.models.responses.MovieResponse;
import com.example.thangcachep.movie_project_be.services.impl.MovieService;

import lombok.RequiredArgsConstructor;

/**
 * Staff Movie Controller
 * Quyền hạn: Staff chỉ có thể tạo và sửa phim, không thể xóa
 * Phim do Staff tạo sẽ có status PENDING (chờ Admin duyệt)
 */
@RestController
@RequestMapping("/api/v1/staff/movies")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StaffMovieController {

    private final MovieService movieService;

    /**
     * Lấy danh sách phim (bao gồm cả pending)
     * GET /api/v1/staff/movies
     */
    @GetMapping
    public ResponseEntity<List<MovieResponse>> getAllMovies() {
        try {
            // Staff xem phim do mình tạo
            List<MovieResponse> movies = movieService.getMoviesByCurrentStaff();
            return ResponseEntity.ok(movies);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Lấy chi tiết phim
     * GET /api/v1/staff/movies/{id}
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
     * Tạo phim mới (status: PENDING)
     * POST /api/v1/staff/movies
     */
    @PostMapping
    public ResponseEntity<MovieResponse> createMovie(@RequestBody MovieRequest request) {
        try {
            MovieResponse movie = movieService.createMovieForStaff(request);
            return ResponseEntity.ok(movie);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Cập nhật phim (chỉ cho phép sửa phim PENDING hoặc phim do Staff tạo)
     * PUT /api/v1/staff/movies/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<MovieResponse> updateMovie(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates
    ) {
        try {
            MovieResponse movie = movieService.updateMovieForStaff(id, updates);
            return ResponseEntity.ok(movie);
        } catch (DataNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            // Lỗi permission
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Staff KHÔNG CÓ quyền DELETE - chỉ Admin mới có
}

