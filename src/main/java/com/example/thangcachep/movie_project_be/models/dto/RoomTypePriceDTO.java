package com.example.thangcachep.movie_project_be.models.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class RoomTypePriceDTO {

    private Integer typeId;

    private BigDecimal price;

    private String currency;          // "VND", ...

    private LocalDate startDate;

    private LocalDate endDate;

}
