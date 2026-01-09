package back.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reports extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "photo_url", length = 255)
    private String photoUrl;

    @Column(length = 20)
    private String status = "PENDING";

    // 생성자
    public Reports(Long clubId, Long reporterId, Long targetId, String reason, String photoUrl) {
        this.clubId = clubId;
        this.reporterId = reporterId;
        this.targetId = targetId;
        this.reason = reason;
        this.photoUrl = photoUrl;
    }

    // 도메인 메서드
    public void approve() {
        this.status = "APPROVED";
    }

    public void reject() {
        this.status = "REJECTED";
    }

    public void process() {
        this.status = "PROCESSED";
    }
}

