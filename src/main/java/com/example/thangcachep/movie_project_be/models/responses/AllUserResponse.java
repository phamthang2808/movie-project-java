package com.example.thangcachep.movie_project_be.models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.thangcachep.movie_project_be.entities.UserEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AllUserResponse {

    private Long userId;

//    @JsonProperty("fullname")
    private String fullName;

    //    @NotBlank(message = "Email is required")
    private String email;

    @JsonProperty("phone_number")
//    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    //    @JsonProperty("password")
//    @NotBlank(message = "Password is required")
//    private String password;


    private String img;

    public AllUserResponse(UserEntity user) {
        this.userId = user.getUserId();
        this.fullName = user.getFullName();
        this.email = user.getEmail();
        this.phoneNumber = user.getPhoneNumber();
        this.img = user.getImg();
    }

}
