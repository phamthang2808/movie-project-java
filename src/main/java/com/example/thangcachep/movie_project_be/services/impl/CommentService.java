package com.example.thangcachep.movie_project_be.services.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.thangcachep.movie_project_be.entities.CommentEntity;
import com.example.thangcachep.movie_project_be.entities.CommentLikeEntity;
import com.example.thangcachep.movie_project_be.entities.MovieEntity;
import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.exceptions.DataNotFoundException;
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
    public CommentResponse createComment(Long movieId, CommentRequest request, UserEntity user) {
        // Kiểm tra movie có tồn tại không
        MovieEntity movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy phim với ID: " + movieId));

        // Validate content
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung comment không được để trống");
        }

        if (request.getContent().length() > 1000) {
            throw new IllegalArgumentException("Nội dung comment không được vượt quá 1000 ký tự");
        }

        // Tạo comment entity
        CommentEntity comment = CommentEntity.builder()
                .movie(movie)
                .user(user)
                .content(request.getContent().trim())
                .isAnonymous(request.getIsAnonymous() != null && request.getIsAnonymous())
                .likeCount(0)
                .isActive(true)
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
     * Lấy danh sách comments của một phim
     * @param movieId ID của phim
     * @param userId ID của user hiện tại (có thể null nếu chưa đăng nhập)
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> getMovieComments(Long movieId, Long userId) {
        // Kiểm tra movie có tồn tại không
        if (!movieRepository.existsById(movieId)) {
            throw new DataNotFoundException("Không tìm thấy phim với ID: " + movieId);
        }

        // Lấy tất cả comments gốc (không có parent) và active
        List<CommentEntity> parentComments = commentRepository.findByMovieIdAndParentIsNullAndIsActive(movieId, true);

        // Map sang CommentResponse và load replies, với thông tin isLiked
        return parentComments.stream()
                .map(comment -> mapToCommentResponseWithReplies(comment, userId))
                .collect(Collectors.toList());
    }

    /**
     * Map CommentEntity sang CommentResponse
     */
    private CommentResponse mapToCommentResponse(CommentEntity comment) {
        CommentResponse.CommentResponseBuilder builder = CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .likeCount(comment.getLikeCount())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt());

        // Nếu ẩn danh, set author và avatar = null
        if (comment.getIsAnonymous() != null && comment.getIsAnonymous()) {
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

        return builder.build();
    }

    /**
     * Map CommentEntity sang CommentResponse với replies
     * @param comment Comment entity
     * @param userId ID của user hiện tại (có thể null)
     */
    private CommentResponse mapToCommentResponseWithReplies(CommentEntity comment, Long userId) {
        CommentResponse response = mapToCommentResponse(comment, userId);

        // Lấy replies của comment này (chỉ lấy active)
        List<CommentEntity> replies = commentRepository.findByParentIdAndIsActive(comment.getId(), true);
        List<CommentResponse> replyResponses = replies.stream()
                .map(reply -> mapToCommentResponse(reply, userId))
                .collect(Collectors.toList());

        response.setReplies(replyResponses);

        return response;
    }

    /**
     * Map CommentEntity sang CommentResponse (overload với userId)
     */
    private CommentResponse mapToCommentResponse(CommentEntity comment, Long userId) {
        CommentResponse response = mapToCommentResponse(comment);

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
            throw new IllegalArgumentException("Bạn phải đăng nhập để like comment");
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
}

