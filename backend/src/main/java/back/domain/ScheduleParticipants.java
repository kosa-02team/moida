package back.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(name = "uk_schedule_user", columnNames = {"schedule_id", "user_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleParticipants extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "participant_id")
    private Long participantId;

    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "attendance_status", length = 20)
    private String attendanceStatus = "UNDECIDED";

    @Column(name = "is_refunded")
    private Boolean isRefunded = false;

    @Column(name = "fee_status", length = 20)
    private String feeStatus = "PENDING";

    @Column(name = "matched_transaction_id")
    private Long matchedTransactionId;

    @Column(name = "fee_request_closed_at")
    private java.time.LocalDateTime feeRequestClosedAt;

    // 생성자
    public ScheduleParticipants(Long scheduleId, Long userId) {
        this.scheduleId = scheduleId;
        this.userId = userId;
    }

    // 도메인 메서드
    public void attend() {
        this.attendanceStatus = "ATTENDING";
    }

    public void notAttend() {
        this.attendanceStatus = "NOT_ATTENDING";
    }

    public void undecided() {
        this.attendanceStatus = "UNDECIDED";
    }

    public void markRefunded() {
        this.isRefunded = true;
    }

    public void resetRefund() {
        this.isRefunded = false;
    }

    public void updateFeeStatus(String feeStatus) {
        this.feeStatus = feeStatus;
    }

    public void matchTransaction(Long transactionId) {
        this.matchedTransactionId = transactionId;
        this.feeStatus = "PAID";
    }

    public void closeFeeRequest() {
        this.feeRequestClosedAt = java.time.LocalDateTime.now();
    }
}

