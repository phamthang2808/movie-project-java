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
public class DetailBookingDTO {

    private Integer bookingId;

    private String serviceName;

    private String serviceDesc;

    private BigDecimal amount;

    private LocalDate newCheckout;
}
