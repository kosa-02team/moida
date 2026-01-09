package back.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Votes extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vote_id")
    private Long voteId;

    @Column(name = "post_id")
    private Long postId;

    @Column(name = "vote_type", length = 20, nullable = false)
    private String voteType = "GENERAL"; // "GENERAL" 또는 "ATTENDANCE"

    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_anonymous")
    private Boolean isAnonymous = false;

    @Column(name = "allow_multiple")
    private Boolean allowMultiple = false;

    @Column(length = 20, nullable = false)
    private String status = "OPEN";

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "deadline")
    private LocalDateTime deadline; // 투표 종료 기한 (일반 투표용)

    // 생성자
    public Votes(Long postId, String voteType, Long scheduleId, Long creatorId, String title, String description, 
                 Boolean isAnonymous, Boolean allowMultiple, LocalDateTime deadline) {
        this.postId = postId;
        this.voteType = voteType != null ? voteType : "GENERAL";
        this.scheduleId = scheduleId;
        this.creatorId = creatorId;
        this.title = title;
        this.description = description;
        this.isAnonymous = isAnonymous != null ? isAnonymous : false;
        this.allowMultiple = allowMultiple != null ? allowMultiple : false;
        this.deadline = deadline;
    }

    // 도메인 메서드
    public void close() {
        this.status = "CLOSED";
        this.closedAt = LocalDateTime.now();
    }

    public void reopen() {
        this.status = "OPEN";
        this.closedAt = null;
    }

    public void updateVote(String title, String description) {
        this.title = title;
        this.description = description;
    }
}

