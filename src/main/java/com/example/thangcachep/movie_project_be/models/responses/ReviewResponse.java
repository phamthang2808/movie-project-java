package com.example.thangcachep.movie_project_be.models.responses;

import com.example.thangcachep.movie_project_be.entities.ReviewEntity;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ReviewResponse {

    private String message;

}
