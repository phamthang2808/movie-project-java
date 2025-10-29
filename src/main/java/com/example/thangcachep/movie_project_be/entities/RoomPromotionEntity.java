package com.example.thangcachep.movie_project_be.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "room_promotions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoomPromotionEntity extends BaseEntity  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;   // Khóa chính mới

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private RoomEntity room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promo_id", nullable = false)
    private PromotionEntity promotion;

    private Integer priority;
    private Boolean stackable;
    private Boolean active;
}
