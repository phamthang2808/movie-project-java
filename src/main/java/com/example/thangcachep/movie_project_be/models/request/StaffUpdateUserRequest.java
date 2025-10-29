package com.example.thangcachep.movie_project_be.models.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;


@Getter
@Setter
@NoArgsConstructor
public class StaffUpdateUserRequest{

    private String fullName;

    private String email;

    private String phoneNumber;

    @NotBlank(message = "Password is required")
    private String password;

    private boolean active;

    private String img;

    private int facebookAccountId;

    private int googleAccountId;

    List<MultipartFile> files;

}
