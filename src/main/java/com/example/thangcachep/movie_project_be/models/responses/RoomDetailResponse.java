package com.example.thangcachep.movie_project_be.models.responses;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class RoomDetailResponse {

//    private Long roomId;

    private String name;

    private String address;

    private String description;

    private String title;

    private Integer guests;

    private Integer size;

    private String beds;

    private String view;

    private Double price;

    private String message;

    private String image;

    private Boolean airConditioning;

    private Boolean wifi;

    private Boolean hairDryer;

    private Boolean petsAllowed;

    private Boolean nonSmoking;

//    private List<MultipartFile> files;

}
