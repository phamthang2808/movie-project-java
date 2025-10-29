package com.example.thangcachep.movie_project_be.models.request;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.thangcachep.movie_project_be.entities.RoleEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class StaffCreateRequest{


    private String fullName;

    @NotBlank(message = "Email is required")
    private String email;


    @NotBlank(message = "Phone number is required")
    private String phoneNumber;


    @NotBlank(message = "Password is required")
    private String password;

    private String retypePassword;

    private String img;

    @JsonProperty("facebook_account_id")
    private int facebookAccountId;

    @JsonProperty("google_account_id")
    private int googleAccountId;

    List<MultipartFile> files;

}
