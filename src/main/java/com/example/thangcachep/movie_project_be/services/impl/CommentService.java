package com.example.thangcachep.movie_project_be.services.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.thangcachep.movie_project_be.entities.CommentEntity;
import com.example.thangcachep.movie_project_be.entities.MovieEntity;
import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.exceptions.DataNotFoundException;
import com.example.thangcachep.movie_project_be.models.request.CommentRequest;
import com.example.thangcachep.movie_project_be.models.responses.CommentResponse;
import com.example.thangcachep.movie_project_be.repositories.CommentRepository;
import com.example.thangcachep.movie_project_be.repositories.MovieRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final MovieRepository movieRepository;

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

        return mapToCommentResponse(comment);
    }

    /**
     * Lấy danh sách comments của một phim
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> getMovieComments(Long movieId) {
        // Kiểm tra movie có tồn tại không
        if (!movieRepository.existsById(movieId)) {
            throw new DataNotFoundException("Không tìm thấy phim với ID: " + movieId);
        }

        // Lấy tất cả comments gốc (không có parent) và active
        List<CommentEntity> parentComments = commentRepository.findByMovieIdAndParentIsNullAndIsActive(movieId, true);

        // Map sang CommentResponse và load replies
        return parentComments.stream()
                .map(this::mapToCommentResponseWithReplies)
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
     */
    private CommentResponse mapToCommentResponseWithReplies(CommentEntity comment) {
        CommentResponse response = mapToCommentResponse(comment);

        // Lấy replies của comment này (chỉ lấy active)
        List<CommentEntity> replies = commentRepository.findByParentIdAndIsActive(comment.getId(), true);
        List<CommentResponse> replyResponses = replies.stream()
                .map(this::mapToCommentResponse)
                .collect(Collectors.toList());

        response.setReplies(replyResponses);

        return response;
    }
}

