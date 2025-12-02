package com.example.thangcachep.movie_project_be.utils;

import com.example.thangcachep.movie_project_be.models.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Utility class để tạo response chuẩn cho các controller
 */
public class ResponseHelper {

    /**
     * Tạo success response với status 200
     */
    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Tạo success response với message tùy chỉnh
     */
    public static <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }

    /**
     * Tạo created response với status 201
     */
    public static <T> ResponseEntity<ApiResponse<T>> created(String message, T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(message, 201, data));
    }

    /**
     * Tạo created response với message mặc định
     */
    public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return created("Created successfully", data);
    }

    /**
     * Tạo no content response với status 204
     */
    public static ResponseEntity<ApiResponse<Void>> noContent() {
        return ResponseEntity.noContent().build();
    }

    /**
     * Tạo error response
     */
    public static <T> ResponseEntity<ApiResponse<T>> error(String message, HttpStatus status) {
        return ResponseEntity.status(status)
                .body(ApiResponse.error(message, status.value()));
    }

    /**
     * Tạo error response với status code tùy chỉnh
     */
    public static <T> ResponseEntity<ApiResponse<T>> error(String message, int status) {
        return ResponseEntity.status(status)
                .body(ApiResponse.error(message, status));
    }
}

