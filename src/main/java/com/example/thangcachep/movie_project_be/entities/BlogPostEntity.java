package com.example.thangcachep.movie_project_be.entities;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "blog_posts")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BlogPostEntity extends BaseEntity  {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long postId;

    @ManyToOne @JoinColumn(name = "staff_id", nullable = false)
    private UserEntity staff;

    private String title;
    private String slug;
    private String summary;
    @Lob
    private String content;
    private String coverImage;
    private String status;
    private Integer readingTime;
    private Integer viewCount;
    private LocalDateTime publishedAt;
}
