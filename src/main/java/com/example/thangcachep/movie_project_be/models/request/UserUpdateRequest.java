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
public class UserUpdateRequest{

    private String fullName;

    private String email;

    private String phoneNumber;

//    @NotBlank(message = "Password is required")
    private String password;

    private String img;

    private Boolean active;

    private RoleEntity role;

    private int facebookAccountId;

    private int googleAccountId;

    List<MultipartFile> files;

}
