package com.example.thangcachep.movie_project_be.repositories;

import com.example.thangcachep.movie_project_be.entities.UserEntity;
import org.apache.catalina.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByEmail(String email);
    Optional<UserEntity> findByPhoneNumber(String phoneNumber);
    Optional<UserEntity> findByUserId(Long id);
    //SELECT * FROM users WHERE phoneNumber=?
    Page<UserEntity> findByRole_RoleId(Pageable pageable, int roleId);
    Page<UserEntity> findAllByRole_RoleId(Long roleId, Pageable pageable);
}

