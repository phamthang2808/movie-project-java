package com.example.thangcachep.movie_project_be.services.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.thangcachep.movie_project_be.entities.CommentEntity;
import com.example.thangcachep.movie_project_be.entities.CommentLikeEntity;
import com.example.thangcachep.movie_project_be.entities.MovieEntity;
import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.exceptions.ConflictException;
import com.example.thangcachep.movie_project_be.exceptions.DataNotFoundException;
import com.example.thangcachep.movie_project_be.exceptions.InvalidParamException;
import com.example.thangcachep.movie_project_be.exceptions.UnauthorizedException;
import com.example.thangcachep.movie_project_be.models.request.CommentRequest;
import com.example.thangcachep.movie_project_be.models.responses.CommentResponse;
import com.example.thangcachep.movie_project_be.repositories.CommentLikeRepository;
import com.example.thangcachep.movie_project_be.repositories.CommentRepository;
import com.example.thangcachep.movie_project_be.repositories.MovieRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final MovieRepository movieRepository;
    private final CommentLikeRepository commentLikeRepository;

    /**
     * Tạo comment mới
     */
    @Transactional
    @CacheEvict(value = {"comments", "movies", "statistics"}, allEntries = true)
    public CommentResponse createComment(Long movieId, CommentRequest request, UserEntity user) {
        // Kiểm tra movie có tồn tại không
        MovieEntity movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy phim với ID: " + movieId));

        // Validate content
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new InvalidParamException("Nội dung comment không được để trống");
        }

        if (request.getContent().length() > 1000) {
            throw new InvalidParamException("Nội dung comment không được vượt quá 1000 ký tự");
        }

        // Tạo comment entity với status APPROVED (tự động hiển thị)
        // Nếu cần moderation, có thể đổi thành PENDING
        CommentEntity comment = CommentEntity.builder()
                .movie(movie)
                .user(user)
                .content(request.getContent().trim())
                .isAnonymous(request.getIsAnonymous() != null ? request.getIsAnonymous() : false)
                .likeCount(0)
                .isActive(true)
                .status(CommentEntity.CommentStatus.APPROVED) // Tự động approve, Admin có thể reject sau nếu vi phạm
                .build();

        // Nếu có parentId, set parent comment
        if (request.getParentId() != null) {
            CommentEntity parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new DataNotFoundException("Không tìm thấy comment cha với ID: " + request.getParentId()));
            comment.setParent(parent);
        }

        // Lưu comment
        comment = commentRepository.save(comment);

        // Trả về response với isLiked = false (comment mới tạo chưa được like)
        Long userId = user != null ? user.getId() : null;
        return mapToCommentResponse(comment, userId);
    }

    /**
     * Lấy danh sách comments của một phim (chỉ lấy APPROVED và active)
     * @param movieId ID của phim
     * @param userId ID của user hiện tại (có thể null nếu chưa đăng nhập)
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> getMovieComments(Long movieId, Long userId) {
        // Kiểm tra movie có tồn tại không
        if (!movieRepository.existsById(movieId)) {
            throw new DataNotFoundException("Không tìm thấy phim với ID: " + movieId);
        }

        // Lấy tất cả comments gốc (không có parent), APPROVED và active
        List<CommentEntity> parentComments = commentRepository.findByMovieIdAndStatus(movieId, CommentEntity.CommentStatus.APPROVED)
                .stream()
                .filter(comment -> comment.getParent() == null && comment.getIsActive() != null && comment.getIsActive())
                .collect(Collectors.toList());

        // Map sang CommentResponse và load replies, với thông tin isLiked
        return parentComments.stream()
                .map(comment -> mapToCommentResponseWithReplies(comment, userId))
                .collect(Collectors.toList());
    }

    /**
     * Lấy replies của comment (chỉ lấy APPROVED và active)
     */
    private List<CommentEntity> getApprovedReplies(Long parentId) {
        return commentRepository.findByParentIdAndIsActive(parentId, true)
                .stream()
                .filter(comment -> comment.getStatus() == CommentEntity.CommentStatus.APPROVED)
                .collect(Collectors.toList());
    }

    /**
     * Map CommentEntity sang CommentResponse (for public API - không có thông tin admin)
     */
    private CommentResponse mapToCommentResponse(CommentEntity comment) {
        return mapToCommentResponse(comment, false);
    }

    /**
     * Map CommentEntity sang CommentResponse
     * @param comment Comment entity
     * @param includeAdminInfo true nếu cần thông tin admin (cho Admin/Staff), false cho public API
     */
    private CommentResponse mapToCommentResponse(CommentEntity comment, boolean includeAdminInfo) {
        CommentResponse.CommentResponseBuilder builder = CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .likeCount(comment.getLikeCount())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt());

        // Nếu ẩn danh, set author và avatar = null
        if (Boolean.TRUE.equals(comment.getIsAnonymous())) {
            builder.author(null);
            builder.avatar(null);
        } else {
            // Nếu không ẩn danh, lấy thông tin user
            UserEntity user = comment.getUser();
            if (user != null) {
                builder.author(user.getName() != null ? user.getName() : user.getEmail());
                builder.avatar(user.getAvatarUrl());
            } else {
                builder.author(null);
                builder.avatar(null);
            }
        }

        // Thêm thông tin admin nếu cần (cho Admin/Staff management)
        if (includeAdminInfo) {
            try {
                // Kiểm tra xem status có tồn tại không (có thể null nếu database chưa có column)
                if (comment.getStatus() != null) {
                    builder.status(comment.getStatus().name());
                } else {
                    // Nếu status null, mặc định là APPROVED (cho comments cũ)
                    builder.status("APPROVED");
                }
            } catch (Exception e) {
                // Nếu có lỗi khi get status (ví dụ: column chưa tồn tại), mặc định APPROVED
                System.err.println("Warning: Cannot get comment status, defaulting to APPROVED: " + e.getMessage());
                builder.status("APPROVED");
            }

            UserEntity user = comment.getUser();
            if (user != null) {
                builder.userId(user.getId());
                builder.userEmail(user.getEmail());
            }

            MovieEntity movie = comment.getMovie();
            if (movie != null) {
                builder.movieId(movie.getId());
                builder.movieTitle(movie.getTitle());
            }

            UserEntity approvedBy = comment.getApprovedBy();
            if (approvedBy != null) {
                builder.approvedById(approvedBy.getId());
                builder.approvedByName(approvedBy.getName() != null ? approvedBy.getName() : approvedBy.getEmail());
            }

            builder.approvedAt(comment.getApprovedAt());
        }

        return builder.build();
    }

    /**
     * Map CommentEntity sang CommentResponse với replies (chỉ APPROVED)
     * @param comment Comment entity
     * @param userId ID của user hiện tại (có thể null)
     */
    private CommentResponse mapToCommentResponseWithReplies(CommentEntity comment, Long userId) {
        CommentResponse response = mapToCommentResponse(comment, userId);

        // Lấy replies của comment này (chỉ lấy APPROVED và active)
        List<CommentEntity> replies = getApprovedReplies(comment.getId());
        List<CommentResponse> replyResponses = replies.stream()
                .map(reply -> mapToCommentResponse(reply, userId))
                .collect(Collectors.toList());

        response.setReplies(replyResponses);

        return response;
    }

    /**
     * Map CommentEntity sang CommentResponse (overload với userId - for public API)
     */
    private CommentResponse mapToCommentResponse(CommentEntity comment, Long userId) {
        return mapToCommentResponse(comment, userId, false);
    }

    /**
     * Map CommentEntity sang CommentResponse (overload với userId và includeAdminInfo)
     */
    private CommentResponse mapToCommentResponse(CommentEntity comment, Long userId, boolean includeAdminInfo) {
        CommentResponse response = mapToCommentResponse(comment, includeAdminInfo);

        // Kiểm tra user đã like comment này chưa
        if (userId != null) {
            boolean isLiked = commentLikeRepository.findByUserIdAndCommentId(userId, comment.getId()).isPresent();
            response.setIsLiked(isLiked);
        } else {
            response.setIsLiked(false);
        }

        return response;
    }

    /**
     * Toggle like/unlike một comment (like nếu chưa like, unlike nếu đã like)
     * @param commentId ID của comment
     * @param user User đang like/unlike (bắt buộc phải có, không thể null)
     * @return CommentResponse với isLiked = true nếu đã like, false nếu đã unlike
     * @throws IllegalArgumentException nếu user == null
     */
    @Transactional
    public CommentResponse toggleLikeComment(Long commentId, UserEntity user) {
        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy comment với ID: " + commentId));

        // Yêu cầu user phải đăng nhập
        if (user == null) {
            throw new UnauthorizedException("Bạn phải đăng nhập để like comment");
        }

        // Kiểm tra user đã like comment này chưa
        var commentLikeOpt = commentLikeRepository.findByUserIdAndCommentId(user.getId(), commentId);

        if (commentLikeOpt.isPresent()) {
            // Đã like rồi → unlike (xóa like)
            CommentLikeEntity commentLike = commentLikeOpt.get();
            commentLikeRepository.delete(commentLike);

            // Giảm likeCount đi 1 (đảm bảo không âm)
            int newLikeCount = Math.max(0, comment.getLikeCount() - 1);
            comment.setLikeCount(newLikeCount);
            comment = commentRepository.save(comment);

            // Trả về comment với isLiked = false
            CommentResponse response = mapToCommentResponse(comment, user.getId());
            response.setIsLiked(false); // Đã unlike
            return response;
        } else {
            // Chưa like → like (thêm like)
            CommentLikeEntity commentLike = CommentLikeEntity.builder()
                    .user(user)
                    .comment(comment)
                    .build();
            commentLikeRepository.save(commentLike);

            // Tăng likeCount lên 1
            comment.setLikeCount(comment.getLikeCount() + 1);
            comment = commentRepository.save(comment);

            // Trả về comment với isLiked = true
            CommentResponse response = mapToCommentResponse(comment, user.getId());
            response.setIsLiked(true); // Đã like
            return response;
        }
    }

    // ==========================================
    // ADMIN/STAFF COMMENT MANAGEMENT METHODS
    // ==========================================

    /**
     * Lấy danh sách tất cả comments (Admin/Staff)
     *
     * - Admin: Xem TẤT CẢ comments của TẤT CẢ phim
     * - Staff: Xem TẤT CẢ comments của TẤT CẢ phim (không phân biệt phim nào)
     *
     * @param search Từ khóa tìm kiếm (content, user name, movie title)
     * @param status Lọc theo status (PENDING, APPROVED, REJECTED)
     * @return Danh sách CommentResponse với đầy đủ thông tin
     * Cache: 10 phút, cache key dựa trên search và status
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "comments", key = "'admin:comments:all:' + (#search != null ? #search : '') + ':' + (#status != null ? #status : 'all')")
    public List<CommentResponse> getAllComments(String search, String status) {
        try {
            List<CommentEntity> comments;

            // Normalize search và status
            String normalizedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
            CommentEntity.CommentStatus commentStatus = null;
            if (status != null && !status.trim().isEmpty()) {
                try {
                    commentStatus = CommentEntity.CommentStatus.valueOf(status.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Invalid status, ignore
                    System.err.println("Invalid status: " + status);
                }
            }

            try {
                // Thử query với status nếu có
                if (normalizedSearch != null && commentStatus != null) {
                    // Search và filter theo status
                    comments = commentRepository.searchCommentsByStatus(normalizedSearch, commentStatus);
                } else if (normalizedSearch != null) {
                    // Chỉ search
                    comments = commentRepository.searchComments(normalizedSearch);
                } else if (commentStatus != null) {
                    // Chỉ filter theo status
                    comments = commentRepository.findByStatus(commentStatus);
                } else {
                    // Lấy tất cả
                    comments = commentRepository.findAll();
                }
            } catch (Exception queryException) {
                // Nếu query lỗi (có thể do database chưa có columns), fallback về query đơn giản
                System.err.println("Warning: Query with status failed, using simple query: " + queryException.getMessage());
                if (normalizedSearch != null) {
                    // Fallback: search không có status
                    comments = commentRepository.findAll().stream()
                            .filter(c ->
                                    c.getContent().toLowerCase().contains(normalizedSearch.toLowerCase()) ||
                                            (c.getUser() != null && c.getUser().getEmail() != null &&
                                                    c.getUser().getEmail().toLowerCase().contains(normalizedSearch.toLowerCase())) ||
                                            (c.getMovie() != null && c.getMovie().getTitle() != null &&
                                                    c.getMovie().getTitle().toLowerCase().contains(normalizedSearch.toLowerCase()))
                            )
                            .collect(Collectors.toList());
                } else {
                    comments = commentRepository.findAll();
                }
            }

            // Fetch user và movie để tránh lazy loading issues
            comments.forEach(comment -> {
                try {
                    if (comment.getUser() != null) {
                        comment.getUser().getName(); // Trigger lazy load
                    }
                    if (comment.getMovie() != null) {
                        comment.getMovie().getTitle(); // Trigger lazy load
                    }
                    if (comment.getApprovedBy() != null) {
                        comment.getApprovedBy().getName(); // Trigger lazy load
                    }
                } catch (Exception e) {
                    System.err.println("Warning: Error loading relations for comment " + comment.getId() + ": " + e.getMessage());
                }
            });

            return comments.stream()
                    .map(comment -> {
                        try {
                            return mapToCommentResponse(comment, null, true); // includeAdminInfo = true
                        } catch (Exception e) {
                            System.err.println("Error mapping comment " + comment.getId() + ": " + e.getMessage());
                            // Return basic response without admin info
                            return mapToCommentResponse(comment, null, false);
                        }
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error in getAllComments: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Không thể lấy danh sách comments. Vui lòng kiểm tra database đã có columns: status, approved_by_user_id, approved_at chưa? Lỗi: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy comment by ID (Admin/Staff)
     */
    @Transactional(readOnly = true)
    public CommentResponse getCommentById(Long commentId) throws DataNotFoundException {
        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy comment với ID: " + commentId));
        return mapToCommentResponse(comment, null, true); // includeAdminInfo = true
    }

    /**
     * Duyệt comment (Admin only)
     * Có thể duyệt comment từ PENDING → APPROVED
     * Hoặc phục hồi comment từ REJECTED → APPROVED
     * @param commentId ID của comment
     * @param admin Admin đang duyệt
     * @return CommentResponse đã được duyệt
     */
    @Transactional
    @CacheEvict(value = {"comments", "movies", "statistics"}, allEntries = true)
    public CommentResponse approveComment(Long commentId, UserEntity admin) throws DataNotFoundException {
        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy comment với ID: " + commentId));

        // Có thể duyệt comment PENDING hoặc phục hồi comment REJECTED
        if (comment.getStatus() == CommentEntity.CommentStatus.APPROVED) {
            throw new ConflictException("Comment đã được duyệt rồi");
        }

        comment.setStatus(CommentEntity.CommentStatus.APPROVED);
        comment.setIsActive(true);
        comment.setApprovedBy(admin);
        comment.setApprovedAt(LocalDateTime.now());
        comment = commentRepository.save(comment);

        return mapToCommentResponse(comment, null, true);
    }

    /**
     * Từ chối comment (Admin only)
     * Có thể từ chối comment từ PENDING → REJECTED
     * Hoặc từ chối comment từ APPROVED → REJECTED (ẩn comment đã hiển thị)
     * @param commentId ID của comment
     * @param admin Admin đang từ chối
     * @return CommentResponse đã bị từ chối
     */
    @Transactional
    @CacheEvict(value = {"comments", "movies", "statistics"}, allEntries = true)
    public CommentResponse rejectComment(Long commentId, UserEntity admin) throws DataNotFoundException {
        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy comment với ID: " + commentId));

        // Có thể từ chối comment PENDING hoặc APPROVED (ẩn comment đã hiển thị)
        if (comment.getStatus() == CommentEntity.CommentStatus.REJECTED) {
            throw new ConflictException("Comment đã bị từ chối rồi");
        }

        comment.setStatus(CommentEntity.CommentStatus.REJECTED);
        comment.setIsActive(false); // Ẩn comment bị từ chối
        comment.setApprovedBy(admin);
        comment.setApprovedAt(LocalDateTime.now());
        comment = commentRepository.save(comment);

        return mapToCommentResponse(comment, null, true);
    }

    /**
     * Xóa comment (hard delete - Admin only)
     * Xóa tất cả likes và replies liên quan trước khi xóa comment
     * Xóa đệ quy tất cả nested replies
     * @param commentId ID của comment
     */
    @Transactional
    @CacheEvict(value = {"comments", "movies", "statistics"}, allEntries = true)
    public void deleteComment(Long commentId) throws DataNotFoundException {
        // Kiểm tra comment có tồn tại không
        if (!commentRepository.existsById(commentId)) {
            throw new DataNotFoundException("Không tìm thấy comment với ID: " + commentId);
        }

        // Xóa đệ quy: xóa tất cả replies và likes trước khi xóa comment
        deleteCommentRecursive(commentId);
    }

    /**
     * Xóa comment và tất cả replies của nó một cách đệ quy
     * @param commentId ID của comment cần xóa
     */
    private void deleteCommentRecursive(Long commentId) {
        // Bước 1: Lấy tất cả replies (child comments) của comment này
        List<CommentEntity> replies = commentRepository.findByParentId(commentId);

        // Bước 2: Xóa đệ quy tất cả replies trước
        // Điều này đảm bảo xóa tất cả nested replies (replies của replies)
        if (replies != null && !replies.isEmpty()) {
            for (CommentEntity reply : replies) {
                // Xóa đệ quy reply này (có thể có replies của chính nó)
                deleteCommentRecursive(reply.getId());
            }
        }

        // Bước 3: Xóa tất cả likes của comment này
        // Điều này cần thiết vì comment_likes có foreign key đến comments
        commentLikeRepository.deleteByCommentId(commentId);

        // Bước 4: Xóa comment chính
        // Bây giờ đã xóa hết các dependencies (replies và likes), có thể xóa comment an toàn
        commentRepository.deleteById(commentId);
    }
}

