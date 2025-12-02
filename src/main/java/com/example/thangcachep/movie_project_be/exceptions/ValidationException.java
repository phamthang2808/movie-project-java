package com.example.thangcachep.movie_project_be.exceptions;

/**
 * Exception khi validation dữ liệu thất bại
 * Status code: 422 Unprocessable Entity
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}

