package com.example.thangcachep.movie_project_be.models.responses;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(builderClassName = "EpisodeResponseBuilder")
@JsonDeserialize(builder = EpisodeResponse.EpisodeResponseBuilder.class)
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

