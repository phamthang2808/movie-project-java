package com.example.thangcachep.movie_project_be.models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.thangcachep.movie_project_be.entities.CustomerEntity;
import com.example.thangcachep.movie_project_be.entities.UserEntity;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class StaffResponse {

    @JsonProperty("message")
    private String message;

    @JsonProperty("errors")
    private List<String> errors;

    @JsonProperty("staff")
    private UserEntity userEntity;

}