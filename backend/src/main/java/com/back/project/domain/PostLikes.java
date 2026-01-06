package com.back.project.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(name = "uk_post_like", columnNames = {"post_id", "user_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLikes {

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "like_id")
    private Long likeId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 생성자
    public PostLikes(Long postId, Long userId) {
        this.postId = postId;
        this.userId = userId;
    }
}

