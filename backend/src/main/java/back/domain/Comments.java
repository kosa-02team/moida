package back.domain;

import back.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comments extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long commentId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "writer_id", nullable = false)
    private Long writerId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 생성자
    public Comments(Long postId, Long writerId, String content) {
        this.postId = postId;
        this.writerId = writerId;
        this.content = content;
    }

    // 도메인 메서드
    public void updateContent(String content) {
        this.content = content;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.deletedAt = null;
    }
}

