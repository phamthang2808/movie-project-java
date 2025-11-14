package com.example.thangcachep.movie_project_be.repositories;

import com.example.thangcachep.movie_project_be.entities.WatchTogetherRoomUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchTogetherRoomUserRepository extends JpaRepository<WatchTogetherRoomUserEntity, WatchTogetherRoomUserEntity.RoomUserId> {

    @Query("SELECT ru FROM WatchTogetherRoomUserEntity ru WHERE ru.room.id = :roomId")
    List<WatchTogetherRoomUserEntity> findByRoomId(@Param("roomId") String roomId);

    @Query("SELECT ru FROM WatchTogetherRoomUserEntity ru WHERE ru.room.id = :roomId AND ru.user.id = :userId")
    Optional<WatchTogetherRoomUserEntity> findByRoomIdAndUserId(@Param("roomId") String roomId, @Param("userId") Long userId);

    void deleteByRoomId(String roomId);

    void deleteByRoomIdAndUserId(String roomId, Long userId);
}

