package com.example.thangcachep.movie_project_be.models.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class BookingDTO {

    private Integer customerId;

    private Integer roomId;

    private LocalDate checkinDate;

    private LocalDate checkoutDate;

    private String status;
}
