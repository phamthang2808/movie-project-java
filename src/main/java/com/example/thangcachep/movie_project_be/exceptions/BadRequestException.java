package com.example.thangcachep.movie_project_be.exceptions;

/**
 * Exception khi request không hợp lệ
 * Status code: 400 Bad Request
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}

