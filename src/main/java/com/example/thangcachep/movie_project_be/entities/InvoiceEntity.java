package com.example.thangcachep.movie_project_be.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InvoiceEntity extends BaseEntity  {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Long invoiceId;

    @ManyToOne @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @ManyToOne @JoinColumn(name = "staff_id")
    private UserEntity staff;

    @ManyToOne @JoinColumn(name = "detail_id")
    private DetailBookingEntity detailBooking;

    private LocalDateTime issuedAt;
    private String status;
    private java.math.BigDecimal subtotal;
    private java.math.BigDecimal discount;
    private java.math.BigDecimal taxAmount;
    private java.math.BigDecimal totalAmount;
    private String note;
}
