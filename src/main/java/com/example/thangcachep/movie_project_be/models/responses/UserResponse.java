package com.example.thangcachep.movie_project_be.models.responses;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String email;
    private String name;
    private String avatarUrl;
    private String role;
    private Double balance;
    private Boolean isVip;
    private LocalDateTime vipExpiredAt;
    private Boolean isEmailVerified;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private String phone;
    private LocalDate birthday;
}


