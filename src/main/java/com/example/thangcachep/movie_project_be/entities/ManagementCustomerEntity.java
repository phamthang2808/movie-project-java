package com.example.thangcachep.movie_project_be.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "management_customers")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ManagementCustomerEntity extends BaseEntity  {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private UserEntity staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

}
