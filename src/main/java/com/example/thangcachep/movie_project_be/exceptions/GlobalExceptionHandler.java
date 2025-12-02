package com.example.thangcachep.movie_project_be.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.thangcachep.movie_project_be.components.LocalizationUtils;
import com.example.thangcachep.movie_project_be.utils.MessageKeys;

import lombok.RequiredArgsConstructor;

/**
 * Global exception handler - Xử lý tập trung tất cả exceptions
 * Tất cả response đều có format: { message, status, data }
 * Sử dụng LocalizationUtils để hỗ trợ đa ngôn ngữ (tiếng Việt và tiếng Anh)
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final LocalizationUtils localizationUtils;

    /**
     * Xử lý lỗi đăng nhập: Email hoặc mật khẩu không đúng
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex) {
        String message = localizationUtils.getLocalizedMessage(MessageKeys.WRONG_EMAIL_PASSWORD);
        ErrorResponse error = ErrorResponse.builder()
                .message(message)
                .status(HttpStatus.UNAUTHORIZED.value())
                .data(null)
                .build();
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Xử lý các lỗi authentication khác
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        String message = localizationUtils.getLocalizedMessage(MessageKeys.WRONG_EMAIL_PASSWORD);
        ErrorResponse error = ErrorResponse.builder()
                .message(message)
                .status(HttpStatus.UNAUTHORIZED.value())
                .data(null)
                .build();
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Xử lý DataNotFoundException
     */
    @ExceptionHandler(DataNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDataNotFoundException(DataNotFoundException ex) {
        // Nếu exception có message tùy chỉnh, dùng message đó, không localize
        // Nếu không, dùng default message đã được localize
        String message = ex.getMessage() != null
                ? ex.getMessage()
                : localizationUtils.getLocalizedMessage(MessageKeys.DATA_NOT_FOUND);
        ErrorResponse error = ErrorResponse.builder()
                .message(message)
                .status(HttpStatus.NOT_FOUND.value())
                .data(null)
                .build();
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * Xử lý InvalidParamException
     */
    @ExceptionHandler(InvalidParamException.class)
    public ResponseEntity<ErrorResponse> handleInvalidParamException(InvalidParamException ex) {
        String message = ex.getMessage() != null
                ? ex.getMessage()
                : localizationUtils.getLocalizedMessage(MessageKeys.INVALID_PARAM);
        ErrorResponse error = ErrorResponse.builder()
                .message(message)
                .status(HttpStatus.BAD_REQUEST.value())
                .data(null)
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Xử lý PermissionDenyException
     */
    @ExceptionHandler(PermissionDenyException.class)
    public ResponseEntity<ErrorResponse> handlePermissionDenyException(PermissionDenyException ex) {
        String message = ex.getMessage() != null
                ? ex.getMessage()
                : localizationUtils.getLocalizedMessage(MessageKeys.PERMISSION_DENIED);
        ErrorResponse error = ErrorResponse.builder()
                .message(message)
                .status(HttpStatus.FORBIDDEN.value())
                .data(null)
                .build();
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    /**
     * Xử lý UnauthorizedException (chưa đăng nhập)
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException ex) {
        String message = ex.getMessage() != null
                ? ex.getMessage()
                : localizationUtils.getLocalizedMessage(MessageKeys.UNAUTHORIZED);
        ErrorResponse error = ErrorResponse.builder()
                .message(message)
                .status(HttpStatus.UNAUTHORIZED.value())
                .data(null)
                .build();
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Xử lý IllegalArgumentException (validation errors)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        String message = ex.getMessage() != null
                ? ex.getMessage()
                : localizationUtils.getLocalizedMessage(MessageKeys.INVALID_PARAM);
        ErrorResponse error = ErrorResponse.builder()
                .message(message)
                .status(HttpStatus.BAD_REQUEST.value())
                .data(null)
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Xử lý ConflictException (409 Conflict - dữ liệu đã tồn tại)
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(ConflictException ex) {
        String message = ex.getMessage() != null
                ? ex.getMessage()
                : localizationUtils.getLocalizedMessage(MessageKeys.CONFLICT);
        ErrorResponse error = ErrorResponse.builder()
                .message(message)
                .status(HttpStatus.CONFLICT.value())
                .data(null)
                .build();
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    /**
     * Xử lý ValidationException (422 Unprocessable Entity - validation thất bại)
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
        String message = ex.getMessage() != null
                ? ex.getMessage()
                : localizationUtils.getLocalizedMessage(MessageKeys.VALIDATION_ERROR);
        ErrorResponse error = ErrorResponse.builder()
                .message(message)
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .data(null)
                .build();
        return new ResponseEntity<>(error, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * Xử lý RuntimeException (Bad Request)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        String message = ex.getMessage() != null
                ? ex.getMessage()
                : localizationUtils.getLocalizedMessage(MessageKeys.RUNTIME_ERROR);
        ErrorResponse error = ErrorResponse.builder()
                .message(message)
                .status(HttpStatus.BAD_REQUEST.value())
                .data(null)
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Xử lý tất cả exceptions khác (Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        String message = ex.getMessage() != null
                ? ex.getMessage()
                : localizationUtils.getLocalizedMessage(MessageKeys.INTERNAL_SERVER_ERROR);
        ErrorResponse error = ErrorResponse.builder()
                .message(message)
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .data(null)
                .build();
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}


