package com.example.thangcachep.movie_project_be.services.impl;

import com.example.thangcachep.movie_project_be.entities.WatchTogetherRoomEntity;
import com.example.thangcachep.movie_project_be.repositories.WatchTogetherRoomRepository;
import com.example.thangcachep.movie_project_be.repositories.WatchTogetherRoomUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service để tự động cleanup các room đã hết hạn hoặc không có activity
 * Chạy mỗi 5 phút để kiểm tra và hủy các room:
 * 1. Room đã hết hạn (expiresAt < now)
 * 2. Room không có activity trong 30 phút và không có user nào
 */
@Service
@RequiredArgsConstructor
public class RoomCleanupService {

    private final WatchTogetherRoomRepository roomRepository;
    private final WatchTogetherRoomUserRepository roomUserRepository;

    /**
     * Cleanup expired rooms và inactive rooms
     * Chạy mỗi 5 phút (300000 milliseconds)
     */
    @Scheduled(fixedRate = 300000) // 5 phút
    @Transactional
    public void cleanupExpiredRooms() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Hủy các room đã hết hạn (expiresAt < now)
        List<WatchTogetherRoomEntity> expiredRooms = roomRepository.findExpiredRooms(now);
        for (WatchTogetherRoomEntity room : expiredRooms) {
            deactivateRoom(room);
        }

        // 2. Hủy các room không có activity trong 30 phút và không có user nào
        LocalDateTime inactiveThreshold = now.minusMinutes(30); // 30 phút không có activity
        List<WatchTogetherRoomEntity> inactiveRooms = roomRepository.findInactiveRooms(inactiveThreshold);

        for (WatchTogetherRoomEntity room : inactiveRooms) {
            // Chỉ hủy nếu không có user nào trong room
            Long userCount = roomRepository.countUsersInRoom(room.getId());
            if (userCount == null || userCount == 0) {
                deactivateRoom(room);
            }
        }
    }

    /**
     * Deactivate room và xóa tất cả users
     */
    private void deactivateRoom(WatchTogetherRoomEntity room) {
        // Xóa tất cả users khỏi room
        roomUserRepository.deleteByRoomId(room.getId());

        // Deactivate room
        room.setIsActive(false);
        roomRepository.save(room);
    }
}

