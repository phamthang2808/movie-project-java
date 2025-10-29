package com.example.thangcachep.movie_project_be.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "invoice_items")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InvoiceItemEntity extends BaseEntity  {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @ManyToOne @JoinColumn(name = "invoice_id", nullable = false)
    private InvoiceEntity invoice;

    private String itemType;
    private String description;
    private java.math.BigDecimal qty;
    private java.math.BigDecimal unitPrice;
    private java.math.BigDecimal taxRate;
    private java.math.BigDecimal lineAmount;
    private java.math.BigDecimal lineDiscount;
}
