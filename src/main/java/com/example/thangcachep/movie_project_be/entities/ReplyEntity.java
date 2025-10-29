package com.example.thangcachep.movie_project_be.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "review_replies")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReplyEntity extends BaseEntity  {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reply_id")
    private Long replyId;

    @ManyToOne @JoinColumn(name = "review_id", nullable = false)
    private ReviewEntity review;

    @ManyToOne
    @JoinColumn(name = "staff_id", nullable = false)
    private UserEntity staff;

    private Integer parentId; // bạn có thể làm @ManyToOne self-reference

    private String content;
    private String visibility;
    private String status;
}
