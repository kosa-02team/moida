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
public class Posts extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long postId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "writer_id", nullable = false)
    private Long writerId;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 생성자
    public Posts(Long clubId, Long writerId, String category, String title, String content) {
        this.clubId = clubId;
        this.writerId = writerId;
        this.category = category;
        this.title = title;
        this.content = content;
    }

    // 도메인 메서드
    public void updatePost(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.deletedAt = null;
    }
}

