package com.example.thangcachep.movie_project_be.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.thangcachep.movie_project_be.entities.CategoryEntity;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    Optional<CategoryEntity> findByName(String name);

    Boolean existsByName(String name);

    // Lấy tất cả categories đang active
    List<CategoryEntity> findByIsActiveTrue();

    // Lấy tất cả categories đang active và sắp xếp theo tên
    List<CategoryEntity> findByIsActiveTrueOrderByNameAsc();
}


