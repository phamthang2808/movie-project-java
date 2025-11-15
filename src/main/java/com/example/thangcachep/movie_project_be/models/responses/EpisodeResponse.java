package com.example.thangcachep.movie_project_be.models.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EpisodeResponse {
    private Long id;
    private Long movieId;
    private Integer episodeNumber;
    private String title;
    private String description;
    private String videoUrl;
    private Integer duration; // phút
    private Boolean isVipOnly;
    private Boolean isActive;
}

