package com.example.thangcachep.movie_project_be.repositories;

import com.example.thangcachep.movie_project_be.entities.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String email);

    Boolean existsByEmail(String email);

    Page<UserEntity> findByIsActive(Boolean isActive, Pageable pageable);

    @Query("SELECT u FROM UserEntity u WHERE u.role.name = :roleName")
    Page<UserEntity> findByRoleName(String roleName, Pageable pageable);

    // Search users by name or email (only when search is provided)
    @Query("SELECT u FROM UserEntity u WHERE " +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<UserEntity> searchUsers(@Param("search") String search);

    // Search users by name or email and filter by role (only when both are provided)
    @Query("SELECT u FROM UserEntity u WHERE " +
            "(LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "LOWER(u.role.name) = LOWER(:roleName)")
    List<UserEntity> searchUsersByRole(@Param("search") String search, @Param("roleName") String roleName);

    // Filter users by role only (case insensitive)
    @Query("SELECT u FROM UserEntity u WHERE LOWER(u.role.name) = LOWER(:roleName)")
    List<UserEntity> findByRoleNameIgnoreCase(@Param("roleName") String roleName);
}


