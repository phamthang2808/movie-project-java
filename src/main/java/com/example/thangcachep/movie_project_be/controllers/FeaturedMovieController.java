package com.example.thangcachep.movie_project_be.controllers;

import java.util.List;

import com.example.thangcachep.movie_project_be.models.dto.FeaturedMovieDTO;
import com.example.thangcachep.movie_project_be.models.responses.ApiResponse;
import com.example.thangcachep.movie_project_be.services.impl.FeaturedMovieService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

/**
 * Controller cho Featured Movies
 * Chỉ có endpoint GET để lấy 6 phim nổi bật
 */
@RestController
@RequestMapping("/api/v1/movies/featured")
@RequiredArgsConstructor
public class FeaturedMovieController {

    private final FeaturedMovieService featuredMovieService;

    /**
     * GET /api/v1/movies/featured
     * Lấy danh sách 6 phim nổi bật đang active
     * Public endpoint - ai cũng có thể xem
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FeaturedMovieDTO>>> getFeaturedMovies() {
        List<FeaturedMovieDTO> featuredMovies = featuredMovieService.getFeaturedMovies();
        ApiResponse<List<FeaturedMovieDTO>> response = ApiResponse.success("Lấy danh sách phim nổi bật thành công", featuredMovies);
        return ResponseEntity.ok(response);
    }
}
