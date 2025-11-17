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
    public ResponseEntity<?> getAllComments(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        try {
            List<CommentResponse> comments = commentService.getAllComments(search, status);
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            e.printStackTrace(); // Log error để debug
            // Trả về error message để frontend có thể hiển thị
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal Server Error",
                            "message", e.getMessage() != null ? e.getMessage() : "Có lỗi xảy ra khi lấy danh sách comments. Vui lòng kiểm tra database đã có columns: status, approved_by_user_id, approved_at chưa?",
                            "details", e.getClass().getSimpleName()
                    ));
        }
    }

    /**
     * Lấy comment by ID
     * GET /api/v1/admin/comments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommentResponse> getCommentById(@PathVariable Long id) {
        try {
            CommentResponse comment = commentService.getCommentById(id);
            return ResponseEntity.ok(comment);
        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Duyệt comment
     * POST /api/v1/admin/comments/{id}/approve
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approveComment(@PathVariable Long id) {
        try {
            UserEntity admin = getCurrentUser();
            CommentResponse comment = commentService.approveComment(id, admin);
            return ResponseEntity.ok(comment);
        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Có lỗi xảy ra: " + e.getMessage()));
        }
    }

    /**
     * Từ chối comment
     * POST /api/v1/admin/comments/{id}/reject
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectComment(@PathVariable Long id) {
        try {
            UserEntity admin = getCurrentUser();
            CommentResponse comment = commentService.rejectComment(id, admin);
            return ResponseEntity.ok(comment);
        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Có lỗi xảy ra: " + e.getMessage()));
        }
    }

    /**
     * Xóa comment (hard delete)
     * DELETE /api/v1/admin/comments/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteComment(@PathVariable Long id) {
        try {
            commentService.deleteComment(id);
            return ResponseEntity.ok(Map.of("message", "Xóa comment thành công"));
        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Có lỗi xảy ra: " + e.getMessage()));
        }
    }

    private UserEntity getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (UserEntity) authentication.getPrincipal();
    }
}

