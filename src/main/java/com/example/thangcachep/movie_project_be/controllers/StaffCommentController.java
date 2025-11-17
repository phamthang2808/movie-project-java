package com.example.thangcachep.movie_project_be.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.thangcachep.movie_project_be.exceptions.DataNotFoundException;
import com.example.thangcachep.movie_project_be.models.responses.CommentResponse;
import com.example.thangcachep.movie_project_be.services.impl.CommentService;

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

    /**
     * Lấy danh sách comments (staff only - chỉ xem, không có quyền duyệt/xóa)
     * Staff xem được TẤT CẢ comments của TẤT CẢ phim (không phân biệt phim nào)
     * GET /api/v1/staff/comments
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
                            "message", e.getMessage() != null ? e.getMessage() : "Có lỗi xảy ra khi lấy danh sách comments"
                    ));
        }
    }

    /**
     * Lấy comment by ID (staff only - chỉ xem)
     * GET /api/v1/staff/comments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommentResponse> getCommentById(@PathVariable Long id) {
        try {
            CommentResponse comment = commentService.getCommentById(id);
            return ResponseEntity.ok(comment);
        } catch (DataNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

