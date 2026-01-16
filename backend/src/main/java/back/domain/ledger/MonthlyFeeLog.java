package back.domain.ledger;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(name = "uk_fee", columnNames = {"club_id", "user_id", "fee_month"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonthlyFeeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fee_log_id")
    private Long feeLogId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "fee_month", nullable = false)
    private LocalDate feeMonth;

    @Column(name = "is_paid")
    private Boolean isPaid = false;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // 생성자
    public MonthlyFeeLog(Long clubId, Long userId, LocalDate feeMonth) {
        this.clubId = clubId;
        this.userId = userId;
        this.feeMonth = feeMonth;
    }

    // 도메인 메서드
    public void markAsPaid() {
        this.isPaid = true;
        this.paidAt = LocalDateTime.now();
    }

    public void markAsUnpaid() {
        this.isPaid = false;
        this.paidAt = null;
    }
}
