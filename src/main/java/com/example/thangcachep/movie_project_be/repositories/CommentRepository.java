package com.example.thangcachep.movie_project_be.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.thangcachep.movie_project_be.entities.CommentEntity;

@Repository
public interface CommentRepository extends JpaRepository<CommentEntity, Long> {

    Page<CommentEntity> findByMovieIdAndIsActive(Long movieId, Boolean isActive, Pageable pageable);

    List<CommentEntity> findByMovieIdAndParentIsNullAndIsActive(Long movieId, Boolean isActive);

    List<CommentEntity> findByParentId(Long parentId);

    // Lấy replies của một comment (chỉ lấy active)
    List<CommentEntity> findByParentIdAndIsActive(Long parentId, Boolean isActive);

    // Queries for Admin/Staff comment management
    List<CommentEntity> findByStatus(CommentEntity.CommentStatus status);

    List<CommentEntity> findByStatusAndIsActive(CommentEntity.CommentStatus status, Boolean isActive);

    // Search comments by content, user name, or movie title
    @Query("SELECT c FROM CommentEntity c " +
            "LEFT JOIN FETCH c.user " +
            "LEFT JOIN FETCH c.movie " +
            "LEFT JOIN FETCH c.approvedBy " +
            "WHERE (:search IS NULL OR :search = '' OR " +
            "LOWER(c.content) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.user.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.user.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.movie.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<CommentEntity> searchComments(@Param("search") String search);

    // Search comments by status and search term
    @Query("SELECT c FROM CommentEntity c " +
            "LEFT JOIN FETCH c.user " +
            "LEFT JOIN FETCH c.movie " +
            "LEFT JOIN FETCH c.approvedBy " +
            "WHERE c.status = :status AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(c.content) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.user.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.user.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.movie.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<CommentEntity> searchCommentsByStatus(@Param("search") String search, @Param("status") CommentEntity.CommentStatus status);

    // Get comments by movie ID and status
    List<CommentEntity> findByMovieIdAndStatus(Long movieId, CommentEntity.CommentStatus status);

    // Get comments by user ID and status
    List<CommentEntity> findByUserIdAndStatus(Long userId, CommentEntity.CommentStatus status);
}


