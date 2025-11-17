package com.example.thangcachep.movie_project_be.models.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {

    private String name;
    private String description;
    private String imageUrl;
    private Boolean isActive;
}

