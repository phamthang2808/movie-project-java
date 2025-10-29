package com.example.thangcachep.movie_project_be.models.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class CustomerDTO {

    private Long customerId;

    private String name;

    private String email;

    private String phone;

    private String note;

    private String img;

    private String address;

    private Long active;

    private List<MultipartFile> files;

}
