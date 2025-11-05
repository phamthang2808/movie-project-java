package com.example.thangcachep.movie_project_be.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
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

@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MovieController {

    private final MovieService movieService;

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
}


