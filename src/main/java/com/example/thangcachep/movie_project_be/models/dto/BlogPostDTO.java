package com.example.thangcachep.movie_project_be.models.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class BlogPostDTO {

    private Integer staffId;

    private String title;

    private String slug;

    private String summary;

    private String content;

    private String coverImage;

    private String status;

    private Integer readingTime;

    private Integer viewCount;

    private LocalDateTime publishedAt;
}
