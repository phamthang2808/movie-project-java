package com.example.thangcachep.movie_project_be.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.thangcachep.movie_project_be.entities.CommentLikeEntity;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLikeEntity, Long> {

    /**
     * Kiểm tra user đã like comment chưa
     */
    Optional<CommentLikeEntity> findByUserIdAndCommentId(Long userId, Long commentId);

    /**
     * Xóa like của user cho comment
     */
    void deleteByUserIdAndCommentId(Long userId, Long commentId);

    /**
     * Đếm số lượng like của một comment
     */
    long countByCommentId(Long commentId);
}

