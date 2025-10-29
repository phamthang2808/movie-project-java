package com.example.thangcachep.movie_project_be.models.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class RoomPromotionDTO {

    private Integer promoId;

    private Integer priority;

    private Boolean stackable;

    private Boolean active;
}
