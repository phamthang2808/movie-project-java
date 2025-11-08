package com.example.thangcachep.movie_project_be.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.thangcachep.movie_project_be.entities.CommentEntity;

@Repository
public interface CommentRepository extends JpaRepository<CommentEntity, Long> {

    Page<CommentEntity> findByMovieIdAndIsActive(Long movieId, Boolean isActive, Pageable pageable);

    List<CommentEntity> findByMovieIdAndParentIsNullAndIsActive(Long movieId, Boolean isActive);

    List<CommentEntity> findByParentId(Long parentId);

    // Lấy replies của một comment (chỉ lấy active)
    List<CommentEntity> findByParentIdAndIsActive(Long parentId, Boolean isActive);
}


