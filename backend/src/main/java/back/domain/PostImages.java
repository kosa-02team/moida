package back.domain;

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

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "image_url", nullable = false, length = 255)
    private String imageUrl;

    // 생성자
    public PostImages(Long postId, String imageUrl) {
        this.postId = postId;
        this.imageUrl = imageUrl;
    }

    // 도메인 메서드
    // 이미지는 불변이므로 수정 메서드 제거
    // 이미지 변경이 필요한 경우 기존 이미지 삭제 후 새로 추가해야 함
}

