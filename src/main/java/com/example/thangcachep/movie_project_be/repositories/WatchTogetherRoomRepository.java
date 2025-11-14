package com.example.thangcachep.movie_project_be.repositories;

import com.example.thangcachep.movie_project_be.entities.WatchTogetherRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchTogetherRoomRepository extends JpaRepository<WatchTogetherRoomEntity, String> {

    @Query("SELECT r FROM WatchTogetherRoomEntity r WHERE r.isActive = true " +
            "AND (:movieId IS NULL OR r.movie.id = :movieId) " +
            "ORDER BY r.createdAt DESC")
    List<WatchTogetherRoomEntity> findActiveRooms(@Param("movieId") Long movieId);

    @Query("SELECT r FROM WatchTogetherRoomEntity r WHERE r.id = :id AND r.isActive = true")
    Optional<WatchTogetherRoomEntity> findActiveRoomById(@Param("id") String id);

    @Query("SELECT COUNT(ru) FROM WatchTogetherRoomUserEntity ru WHERE ru.room.id = :roomId")
    Long countUsersInRoom(@Param("roomId") String roomId);
}

