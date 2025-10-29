package com.example.thangcachep.movie_project_be.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class UserDTO{

    @JsonProperty("fullname")
    private String fullName;

    @NotBlank(message = "Email is required")
    private String email;

    @JsonProperty("phone_number")
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @JsonProperty("password")
    @NotBlank(message = "Password is required")
    private String password;

    @JsonProperty("retype_password")
    @NotBlank(message = "Retype password is required")
    private String retypePassword;

    private String img;
//    @NotNull(message = "Role ID is required")
    @JsonProperty("role_id")
    private Long roleId;

    private String roleName;

    @JsonProperty("facebook_account_id")
    private int facebookAccountId;

    @JsonProperty("google_account_id")
    private int googleAccountId;

    List<MultipartFile> files;

}
