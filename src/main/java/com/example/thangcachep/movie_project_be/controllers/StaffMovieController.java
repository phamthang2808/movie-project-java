package com.example.thangcachep.movie_project_be.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.thangcachep.movie_project_be.components.LocalizationUtils;
import com.example.thangcachep.movie_project_be.models.request.MovieRequest;
import com.example.thangcachep.movie_project_be.models.responses.ApiResponse;
import com.example.thangcachep.movie_project_be.models.responses.MovieResponse;
import com.example.thangcachep.movie_project_be.services.impl.MovieService;
import com.example.thangcachep.movie_project_be.utils.MessageKeys;

import lombok.RequiredArgsConstructor;

/**
 * Staff Movie Controller
 * Quyền hạn: Staff chỉ có thể tạo và sửa phim, không thể xóa
 * Phim do Staff tạo sẽ có status PENDING (chờ Admin duyệt)
 * Sử dụng @PreAuthorize để kiểm tra quyền truy cập
 */
@RestController
@RequestMapping("/api/v1/staff/movies")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')") // ADMIN hoặc STAFF
public class StaffMovieController {

    private final MovieService movieService;
    private final LocalizationUtils localizationUtils;

    /**
     * Lấy danh sách phim (bao gồm cả pending)
     * GET /api/v1/staff/movies
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<MovieResponse>>> getAllMovies() {
        // Staff xem phim do mình tạo
        List<MovieResponse> movies = movieService.getMoviesByCurrentStaff();
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_GET_ALL_SUCCESS);
        ApiResponse<List<MovieResponse>> response = ApiResponse.success(message, movies);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy chi tiết phim
     * GET /api/v1/staff/movies/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MovieResponse>> getMovieById(@PathVariable Long id) {
        MovieResponse movie = movieService.getMovieById(id);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_GET_SUCCESS);
        ApiResponse<MovieResponse> response = ApiResponse.success(message, movie);
        return ResponseEntity.ok(response);
    }

    /**
     * Tạo phim mới (status: PENDING)
     * POST /api/v1/staff/movies
     */
    @PostMapping
    public ResponseEntity<ApiResponse<MovieResponse>> createMovie(@RequestBody MovieRequest request) {
        MovieResponse movie = movieService.createMovieForStaff(request);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_CREATE_SUCCESS);
        ApiResponse<MovieResponse> response = ApiResponse.success(message, 201, movie);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Cập nhật phim (chỉ cho phép sửa phim PENDING hoặc phim do Staff tạo)
     * PUT /api/v1/staff/movies/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MovieResponse>> updateMovie(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates
    ) {
        MovieResponse movie = movieService.updateMovieForStaff(id, updates);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.MOVIE_UPDATE_SUCCESS);
        ApiResponse<MovieResponse> response = ApiResponse.success(message, movie);
        return ResponseEntity.ok(response);
    }

    // Staff KHÔNG CÓ quyền DELETE - chỉ Admin mới có
}

