package com.example.thangcachep.movie_project_be.models.responses;

import com.example.thangcachep.movie_project_be.entities.RoomTypeEntity;
import com.example.thangcachep.movie_project_be.models.dto.RoomTypeDTO;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ManagePriceResponse {

    private Long roomId;

    private String name;

    private String description;

    private String title;

    private Double price;

    private Double oldPrice;

    private Integer discount;

    private String typeName;

}
