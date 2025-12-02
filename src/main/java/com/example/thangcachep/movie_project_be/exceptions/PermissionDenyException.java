package com.example.thangcachep.movie_project_be.exceptions;

/**
 * Exception khi không có quyền thực hiện hành động
 * Extends RuntimeException để KHÔNG CẦN khai báo throws
 */
public class PermissionDenyException extends RuntimeException {
    public PermissionDenyException(String message) {
        super(message);
    }
}