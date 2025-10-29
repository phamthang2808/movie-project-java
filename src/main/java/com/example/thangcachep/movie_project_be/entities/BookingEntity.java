package com.example.thangcachep.movie_project_be.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookingEntity extends BaseEntity  {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Long bookingId;

    @ManyToOne @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @ManyToOne @JoinColumn(name = "room_id", nullable = false)
    private RoomEntity room;

    private LocalDate checkinDate;
    private LocalDate checkoutDate;
    private String status;
}
