package com.example.thangcachep.movie_project_be.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.exceptions.DataNotFoundException;
import com.example.thangcachep.movie_project_be.models.responses.ApiResponse;
import com.example.thangcachep.movie_project_be.models.responses.CommentResponse;
import com.example.thangcachep.movie_project_be.services.impl.CommentService;

import lombok.RequiredArgsConstructor;

/**
 * Admin Comment Controller
 * Quyền hạn: Chỉ ADMIN mới có quyền quản lý comments (duyệt, xóa)
 * Sử dụng @PreAuthorize để kiểm tra quyền truy cập
 */
@RestController
@RequestMapping("/api/v1/admin/comments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')") // Chỉ ADMIN
public class AdminCommentController {

    private final CommentService commentService;

    /**
     * Lấy danh sách tất cả comments (admin only)
     * GET /api/v1/admin/comments
     * Query params: search (tìm kiếm), status (lọc theo status)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getAllComments(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        List<CommentResponse> comments = commentService.getAllComments(search, status);
        ApiResponse<List<CommentResponse>> response = ApiResponse.success("Lấy danh sách comments thành công", comments);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy comment by ID
     * GET /api/v1/admin/comments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CommentResponse>> getCommentById(@PathVariable Long id) {
        CommentResponse comment = commentService.getCommentById(id);
        ApiResponse<CommentResponse> response = ApiResponse.success("Lấy comment thành công", comment);
        return ResponseEntity.ok(response);
    }

    /**
     * Duyệt comment
     * POST /api/v1/admin/comments/{id}/approve
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<CommentResponse>> approveComment(@PathVariable Long id) {
        UserEntity admin = getCurrentUser();
        CommentResponse comment = commentService.approveComment(id, admin);
        ApiResponse<CommentResponse> response = ApiResponse.success("Duyệt comment thành công", comment);
        return ResponseEntity.ok(response);
    }

    /**
     * Từ chối comment
     * POST /api/v1/admin/comments/{id}/reject
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<CommentResponse>> rejectComment(@PathVariable Long id) {
        UserEntity admin = getCurrentUser();
        CommentResponse comment = commentService.rejectComment(id, admin);
        ApiResponse<CommentResponse> response = ApiResponse.success("Từ chối comment thành công", comment);
        return ResponseEntity.ok(response);
    }

    /**
     * Xóa comment (hard delete)
     * DELETE /api/v1/admin/comments/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        ApiResponse<Void> response = ApiResponse.success("Xóa comment thành công", null);
        return ResponseEntity.ok(response);
    }

    private UserEntity getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (UserEntity) authentication.getPrincipal();
    }
}

