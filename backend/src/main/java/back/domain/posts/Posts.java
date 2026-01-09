package back.domain.posts;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PostCategory  category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "schedule_id")
    private Long scheduleId; // nullable

    @Column(length = 30)
    private String place;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private Long deletedBy;

    //title 자동 생성
    private static String makeTitleFromContent(String content) {
        String trimmed = content == null ? "" : content.trim();

        // 첫 줄 추출 (\r\n, \n 모두 대응)
        String firstLine = trimmed.split("\\R", 2)[0].trim();

        // 첫 줄이 너무 짧거나 비어있으면 전체에서 생성
        String base = firstLine.isEmpty() ? trimmed : firstLine;

        // 길이 제한
        if (base.length() > 200) {
            base = base.substring(0, 20).trim();
        }

        return base;
    }

    // 생성자
    public Posts(Long clubId, Long writerId, PostCategory category, String title, String content ) {
        this.clubId = clubId;
        this.writerId = writerId;
        this.category = category;
        this.title = title;
        this.content = content;
    }

    public static Posts story(
            Long writerId,
            Long clubId,
            Long scheduleId,
            String content
    ) {

        String title = makeTitleFromContent(content);
        PostCategory category = (scheduleId == null) ? PostCategory.GENERAL : PostCategory.SCHEDULE;

        Posts post = new Posts(clubId, writerId, category, title, content);

        return post;
    }

    // 도메인 메서드
    public void updatePost(String title, String content) {
        this.title = title;
        this.content = content;
    }

    // 도메인 메서드
    public void blindPost(Long actorId) {
        this.deletedBy = actorId;
        this.deletedAt = LocalDateTime.now();
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }


    public void updatePlace(String place){
        this.place = place.trim();
    }

}

