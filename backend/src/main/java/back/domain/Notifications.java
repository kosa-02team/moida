package back.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notifications extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "noti_id")
    private Long notiId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "ref_type", length = 30)
    private String refType;

    @Column(name = "is_read")
    private Boolean isRead = false;

    // 생성자
    public Notifications(Long userId, String content, Long refId, String refType) {
        this.userId = userId;
        this.content = content;
        this.refId = refId;
        this.refType = refType;
    }

    // 도메인 메서드
    public void markAsRead() {
        this.isRead = true;
    }

    public void markAsUnread() {
        this.isRead = false;
    }
}

