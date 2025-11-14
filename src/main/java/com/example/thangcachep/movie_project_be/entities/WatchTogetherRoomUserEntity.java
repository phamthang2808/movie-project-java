package com.example.thangcachep.movie_project_be.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity cho bảng watch_together_room_users
 * Lưu trữ thông tin users trong mỗi room
 */
@Entity
@Table(name = "watch_together_room_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(WatchTogetherRoomUserEntity.RoomUserId.class)
public class WatchTogetherRoomUserEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private WatchTogetherRoomEntity room;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
    }

    // Composite key class
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomUserId implements java.io.Serializable {
        private String room;
        private Long user;
    }
}

