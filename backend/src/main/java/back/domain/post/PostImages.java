package back.domain.post;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImages {

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long imageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Posts post;

    @Column(name = "image_url", nullable = false, length = 255)
    private String imageUrl;

    // 생성자
    private PostImages(Posts post, String imageUrl) {
        this.post = post;
        this.imageUrl = imageUrl;
    }

    public static PostImages of(Posts post, String imageUrl) {
        return new PostImages(post, imageUrl);
    }


    public Long getPostId() {
        return post.getPostId();
    }

}

