package com.example.thangcachep.movie_project_be.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReviewEntity extends BaseEntity  {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    @ManyToOne @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @ManyToOne @JoinColumn(name = "room_id", nullable = false)
    private RoomEntity room;

    @OneToMany(mappedBy = "review") //map toi doi tuong
    private List<ReplyEntity> reviewReply;

    private Long rating;
    
    private String comment;
}
