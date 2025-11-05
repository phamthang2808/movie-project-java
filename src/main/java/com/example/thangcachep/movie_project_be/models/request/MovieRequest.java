package com.example.thangcachep.movie_project_be.models.request;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieRequest {

    private String title;
    private String description;
    private String posterUrl;
    private String backdropUrl;
    private String trailerUrl;
    private String videoUrl;
    private String type; // MOVIE, SERIES
    private String status; // UPCOMING, AIRING, COMPLETED
    private Integer duration;
    private LocalDate releaseDate;
    private Double price;
    private Boolean isVipOnly;
    private List<Long> categoryIds; // IDs của categories
}

