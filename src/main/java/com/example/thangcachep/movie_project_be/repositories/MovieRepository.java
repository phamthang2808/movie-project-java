package com.example.thangcachep.movie_project_be.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.thangcachep.movie_project_be.entities.MovieEntity;

@Repository
public interface MovieRepository extends JpaRepository<MovieEntity, Long> {

    Page<MovieEntity> findByIsActive(Boolean isActive, Pageable pageable);

    Page<MovieEntity> findByType(MovieEntity.MovieType type, Pageable pageable);

    // Lấy phim theo type và chỉ lấy phim active
    Page<MovieEntity> findByTypeAndIsActiveTrue(MovieEntity.MovieType type, Pageable pageable);

    @Query("SELECT m FROM MovieEntity m WHERE m.isActive = true ORDER BY m.rating DESC")
    List<MovieEntity> findTopRatedMovies(Pageable pageable);

    @Query("SELECT m FROM MovieEntity m WHERE m.isActive = true ORDER BY m.viewCount DESC")
    List<MovieEntity> findTopViewedMovies(Pageable pageable);

    @Query("SELECT m FROM MovieEntity m WHERE m.isActive = true AND m.releaseDate >= :date ORDER BY m.releaseDate ASC")
    List<MovieEntity> findUpcomingMovies(LocalDate date, Pageable pageable);

    @Query("SELECT m FROM MovieEntity m WHERE m.isActive = true AND " +
            "(LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(m.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<MovieEntity> searchMovies(String keyword, Pageable pageable);

    @Query("SELECT m FROM MovieEntity m JOIN m.categories c WHERE c.id = :categoryId AND m.isActive = true")
    Page<MovieEntity> findByCategoryId(Long categoryId, Pageable pageable);

    // Lấy tất cả phim active (không phân trang)
    List<MovieEntity> findByIsActiveTrue();

    /**
     * Lấy tất cả phim active với categories đã được fetch sẵn (tối ưu N+1 query)
     * Sử dụng JOIN FETCH để load categories trong cùng một query
     */
    @Query("SELECT DISTINCT m FROM MovieEntity m LEFT JOIN FETCH m.categories WHERE m.isActive = true")
    List<MovieEntity> findByIsActiveTrueWithCategories();

    /**
     * Lấy tất cả phim (kể cả inactive) với categories đã được fetch sẵn
     */
    @Query("SELECT DISTINCT m FROM MovieEntity m LEFT JOIN FETCH m.categories")
    List<MovieEntity> findAllWithCategories();

    /**
     * Lấy phim theo ID với categories đã được fetch sẵn (tối ưu)
     */
    @Query("SELECT DISTINCT m FROM MovieEntity m LEFT JOIN FETCH m.categories WHERE m.id = :id")
    Optional<MovieEntity> findByIdWithCategories(Long id);

    /**
     * Lấy nhiều phim theo list IDs với categories đã được fetch sẵn (tối ưu)
     */
    @Query("SELECT DISTINCT m FROM MovieEntity m LEFT JOIN FETCH m.categories WHERE m.id IN :ids")
    List<MovieEntity> findByIdsWithCategories(List<Long> ids);
}


