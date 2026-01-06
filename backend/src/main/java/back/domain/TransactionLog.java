package back.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransactionLog {

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "editor_id")
    private Long editorId;

    // 생성자
    public TransactionLog(Long clubId, Long accountId, String type, 
                          BigDecimal amount, BigDecimal balanceAfter,
                          String description, Long editorId) {
        this.clubId = clubId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.editorId = editorId;
    }

    // 도메인 메서드
    // 회계 원장은 불변이므로 수정 메서드 제거
    // 오류 수정이 필요한 경우 상계 거래(compensating entry)를 추가해야 함
}
