package com.example.thangcachep.movie_project_be.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roomtypes_prices")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoomTypePriceEntity extends BaseEntity  {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "price_id")
    private Long priceId;

    @ManyToOne @JoinColumn(name = "type_id", nullable = false)
    private RoomTypeEntity roomType;

    private java.math.BigDecimal price;
    private String currency;
    private java.time.LocalDate startDate;
    private java.time.LocalDate endDate;

}
