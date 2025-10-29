package com.example.thangcachep.movie_project_be.models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class AllStaffResponse {

    private Long staffId;

//    @JsonProperty("fullname")
    private String fullName;

    //    @NotBlank(message = "Email is required")
    private String email;

//    @JsonProperty("phone_number")
//    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    //    @JsonProperty("password")
//    @NotBlank(message = "Password is required")
    private String password;


    private String img;


}

