package back.domain.post;

import back.domain.*;
import back.domain.club.ClubMembers;
import back.domain.club.Clubs;
import back.domain.schedule.Schedules;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", nullable = false)
    private Clubs club;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "writer_id", nullable = false)
    private ClubMembers writer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PostCategory  category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private Schedules schedule;

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
    private Posts(Clubs club, ClubMembers writer, Schedules schedule,
                  PostCategory category, String title, String content) {
        this.club = club;
        this.writer = writer;
        this.schedule = schedule;
        this.category = category;
        this.title = title;
        this.content = content;
    }

    public static Posts story(Clubs club, ClubMembers writer, Schedules schedule, String content) {
        String title = makeTitleFromContent(content);
        PostCategory category = (schedule == null) ? PostCategory.GENERAL : PostCategory.SCHEDULE;
        return new Posts(club, writer, schedule, category, title, content);
    }

    public static Posts vote(Clubs club, ClubMembers writer, Schedules schedule, String title, String description) {
        return new Posts(
                club,
                writer,
                schedule,
                PostCategory.VOTE,
                title,
                description
        );
    }

    // 도메인 메서드
    public void updateStory(String content) {
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

