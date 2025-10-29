package com.example.thangcachep.movie_project_be.repositories;

import com.example.thangcachep.movie_project_be.entities.RoomEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<RoomEntity, Long> {
    @Query("""
        SELECT r FROM RoomEntity r
        WHERE (:guests   IS NULL OR r.guests >= :guests)
          AND (:minPrice IS NULL OR r.price  >= :minPrice)
          AND (:maxPrice IS NULL OR r.price  <= :maxPrice)
        ORDER BY r.roomId ASC
    """)
    Page<RoomEntity> searchRooms(Integer guests, Double minPrice, Double maxPrice, Pageable pageable);
    Optional<List<RoomEntity>> findAllByRoomId(Long roomId);
    Optional<RoomEntity> findByRoomId(Long roomId);


}
