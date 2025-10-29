package com.example.thangcachep.movie_project_be.models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.thangcachep.movie_project_be.entities.UserEntity;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterResponse {
    @JsonProperty("message")
    private String message;

    @JsonProperty("user")
    private UserEntity user;
}
