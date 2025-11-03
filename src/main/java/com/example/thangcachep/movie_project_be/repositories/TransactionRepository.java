package com.example.thangcachep.movie_project_be.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.thangcachep.movie_project_be.entities.TransactionEntity;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    Page<TransactionEntity> findByUserId(Long userId, Pageable pageable);

    List<TransactionEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<TransactionEntity> findByPaymentId(String paymentId);
}
