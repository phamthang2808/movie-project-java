package com.example.thangcachep.movie_project_be.exceptions;

/**
 * Exception khi user chưa đăng nhập hoặc không có quyền truy cập
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}

