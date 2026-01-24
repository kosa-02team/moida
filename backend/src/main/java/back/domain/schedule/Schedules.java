package back.domain.schedule;

import back.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "schedules")
public class Schedules extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "schedule_name", nullable = false, length = 200)
    private String scheduleName;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(length = 255)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "entry_fee", precision = 19, scale = 2)
    private BigDecimal entryFee = BigDecimal.ZERO;

    @Column(name = "total_spent", precision = 19, scale = 2)
    private BigDecimal totalSpent = BigDecimal.ZERO;

    @Column(name = "refund_per_person", precision = 19, scale = 2)
    private BigDecimal refundPerPerson = BigDecimal.ZERO;

    @Column(length = 20, nullable = false)
    private String status = "OPEN";

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Setter
    @Column(name = "vote_deadline")
    private LocalDateTime voteDeadline;

    @Column(name = "snapshot_transaction_id")
    private Long snapshotTransactionId;

    @Column(name = "attendance_closed_at")
    private LocalDateTime attendanceClosedAt;

    // 생성자
    public Schedules(Long clubId, String scheduleName, LocalDateTime eventDate, LocalDateTime endDate, String location,
            String description, BigDecimal entryFee, LocalDateTime voteDeadline) {
        this.clubId = clubId;
        this.scheduleName = scheduleName;
        this.eventDate = eventDate;
        this.endDate = endDate;
        this.location = location;
        this.description = description;
        this.entryFee = entryFee != null ? entryFee : BigDecimal.ZERO;
        this.voteDeadline = voteDeadline;
    }

    // 도메인 메서드
    public void update(String scheduleName, LocalDateTime eventDate, LocalDateTime endDate, String location,
            String description, BigDecimal entryFee, LocalDateTime voteDeadline) {
        this.scheduleName = scheduleName;
        this.eventDate = eventDate;
        this.endDate = endDate;
        this.location = location;
        this.description = description;
        this.entryFee = entryFee;
        this.voteDeadline = voteDeadline;
    }

    public void updateSettlement(BigDecimal totalSpent, BigDecimal refundPerPerson) {
        this.totalSpent = totalSpent;
        this.refundPerPerson = refundPerPerson;
    }

    public void closeAttendance() {
        this.attendanceClosedAt = LocalDateTime.now();
    }

    public void close() {
        this.status = "CLOSED";
        this.closedAt = LocalDateTime.now();
    }

    public void cancel(String cancelReason) {
        this.status = "CANCELLED";
        this.closedAt = LocalDateTime.now();
        this.cancelReason = cancelReason;
    }

    public void cancel() {
        this.status = "CANCELLED";
        this.closedAt = LocalDateTime.now();
    }

    public void reopen() {
        this.status = "OPEN";
        this.closedAt = null;
    }

}
