package com.example.thangcachep.movie_project_be.models.request;

import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ReviewRequest {

    private Long roomId;

    private String name;

//    private Long customerId;

    private Long rating;

    private String comment;

}