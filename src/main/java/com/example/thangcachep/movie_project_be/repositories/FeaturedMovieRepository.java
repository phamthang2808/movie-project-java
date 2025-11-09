package com.example.thangcachep.movie_project_be.repositories;

import com.example.thangcachep.movie_project_be.entities.FeaturedMovieEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeaturedMovieRepository extends JpaRepository<FeaturedMovieEntity, Long> {

    /**
     * Tìm featured movie theo movie_id
     */
    Optional<FeaturedMovieEntity> findByMovieId(Long movieId);

    /**
     * Lấy tất cả featured movies đang active, sắp xếp theo displayOrder
     */
    @Query("SELECT fm FROM FeaturedMovieEntity fm " +
            "WHERE fm.isActive = true " +
            "ORDER BY fm.displayOrder ASC")
    List<FeaturedMovieEntity> findAllActiveOrderByDisplayOrder();

    /**
     * Lấy tất cả featured movies (bao gồm inactive), sắp xếp theo displayOrder
     */
    @Query("SELECT fm FROM FeaturedMovieEntity fm " +
            "ORDER BY fm.displayOrder ASC")
    List<FeaturedMovieEntity> findAllOrderByDisplayOrder();

    /**
     * Đếm số lượng featured movies đang active
     */
    long countByIsActiveTrue();

    /**
     * Kiểm tra xem displayOrder đã được sử dụng chưa (với phim active)
     */
    @Query("SELECT COUNT(fm) > 0 FROM FeaturedMovieEntity fm " +
            "WHERE fm.displayOrder = :displayOrder AND fm.isActive = true")
    boolean existsByDisplayOrderAndIsActiveTrue(Integer displayOrder);

    /**
     * Tìm featured movie theo displayOrder và đang active
     */
    Optional<FeaturedMovieEntity> findByDisplayOrderAndIsActiveTrue(Integer displayOrder);
}

