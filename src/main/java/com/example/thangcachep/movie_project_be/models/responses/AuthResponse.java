package com.example.thangcachep.movie_project_be.models.responses;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    
    private String token;
    private String refreshToken;
    private String type = "Bearer";
    private UserResponse user;
}


