package com.example.thangcachep.movie_project_be.models.request;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class BookingInforRequest {

    private Long roomId;

    private String name;

    private String email;

    private String phone;

//    private Long customerId;

    private LocalDate checkinDate;

    private LocalDate checkoutDate;

    private String status;
}
