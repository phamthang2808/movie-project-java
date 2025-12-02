package com.example.thangcachep.movie_project_be.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.thangcachep.movie_project_be.components.LocalizationUtils;
import com.example.thangcachep.movie_project_be.models.responses.ApiResponse;
import com.example.thangcachep.movie_project_be.models.responses.CommentResponse;
import com.example.thangcachep.movie_project_be.services.impl.CommentService;
import com.example.thangcachep.movie_project_be.utils.MessageKeys;

import lombok.RequiredArgsConstructor;

/**
 * Staff Comment Controller
 * Quyền hạn: ADMIN hoặc STAFF có thể xem comments
 * Sử dụng @PreAuthorize để kiểm tra quyền truy cập
 */
@RestController
@RequestMapping("/api/v1/staff/comments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')") // ADMIN hoặc STAFF
public class StaffCommentController {

    private final CommentService commentService;
    private final LocalizationUtils localizationUtils;

    /**
     * Lấy danh sách comments (staff only - chỉ xem, không có quyền duyệt/xóa)
     * Staff xem được TẤT CẢ comments của TẤT CẢ phim (không phân biệt phim nào)
     * GET /api/v1/staff/comments
     * Query params: search (tìm kiếm), status (lọc theo status)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getAllComments(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        List<CommentResponse> comments = commentService.getAllComments(search, status);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.COMMENT_GET_SUCCESS);
        ApiResponse<List<CommentResponse>> response = ApiResponse.success(message, comments);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy comment by ID (staff only - chỉ xem)
     * GET /api/v1/staff/comments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CommentResponse>> getCommentById(@PathVariable Long id) {
        CommentResponse comment = commentService.getCommentById(id);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.COMMENT_GET_SUCCESS);
        ApiResponse<CommentResponse> response = ApiResponse.success(message, comment);
        return ResponseEntity.ok(response);
    }
}

