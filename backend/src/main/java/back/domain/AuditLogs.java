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
public class AuditLogs {

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "before_description", columnDefinition = "TEXT")
    private String beforeDescription;

    @Column(name = "after_description", columnDefinition = "TEXT")
    private String afterDescription;

    // 생성자
    public AuditLogs(Long transactionId, Long actorId, String beforeDescription, String afterDescription) {
        this.transactionId = transactionId;
        this.actorId = actorId;
        this.beforeDescription = beforeDescription;
        this.afterDescription = afterDescription;
    }
}

