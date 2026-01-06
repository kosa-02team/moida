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
public class ScheduleParticipants {

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
}

