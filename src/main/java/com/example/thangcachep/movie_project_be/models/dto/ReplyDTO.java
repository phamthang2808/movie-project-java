package com.example.thangcachep.movie_project_be.models.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ReplyDTO {


    private Long staffId;

    private String content;

    private Long reviewId;

    private String visibility;

    private String status;
}
