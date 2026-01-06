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
public class Votes extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vote_id")
    private Long voteId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

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

    @Column(length = 20)
    private String status = "OPEN";

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    // 생성자
    public Votes(Long postId, Long creatorId, String title, String description, 
                 Boolean isAnonymous, Boolean allowMultiple) {
        this.postId = postId;
        this.creatorId = creatorId;
        this.title = title;
        this.description = description;
        this.isAnonymous = isAnonymous != null ? isAnonymous : false;
        this.allowMultiple = allowMultiple != null ? allowMultiple : false;
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

