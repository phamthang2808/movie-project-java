package com.example.thangcachep.movie_project_be.models.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response chuẩn cho tất cả API
 * Format: { message, status, data }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {

    private String message;
    private int status;
    private T data;

    /**
     * Helper method để tạo success response
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .message("Success")
                .status(200)
                .data(data)
                .build();
    }

    /**
     * Helper method để tạo success response với message tùy chỉnh
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .message(message)
                .status(200)
                .data(data)
                .build();
    }

    /**
     * Helper method để tạo success response với status code tùy chỉnh
     */
    public static <T> ApiResponse<T> success(String message, int status, T data) {
        return ApiResponse.<T>builder()
                .message(message)
                .status(status)
                .data(data)
                .build();
    }

    /**
     * Helper method để tạo error response
     */
    public static <T> ApiResponse<T> error(String message, int status) {
        return ApiResponse.<T>builder()
                .message(message)
                .status(status)
                .data(null)
                .build();
    }
}

