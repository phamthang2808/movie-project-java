package com.example.thangcachep.movie_project_be.models.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import lombok.*;

@Getter
@Setter
public class GoogleAuthRequest {
    @NotBlank(message = "Authorization code không được để trống")
    private String code;
}

