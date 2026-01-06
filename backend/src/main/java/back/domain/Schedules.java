package back.domain;

import back.domain.Posts;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedules {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "post_id")
    private Posts post;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(length = 255)
    private String location;

    @Column(name = "entry_fee", precision = 19, scale = 2)
    private BigDecimal entryFee = BigDecimal.ZERO;

    @Column(name = "total_spent", precision = 19, scale = 2)
    private BigDecimal totalSpent = BigDecimal.ZERO;

    @Column(name = "refund_per_person", precision = 19, scale = 2)
    private BigDecimal refundPerPerson = BigDecimal.ZERO;

    @Column(length = 20)
    private String status = "OPEN";

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    // 생성자
    public Schedules(Posts post, LocalDateTime eventDate, String location, BigDecimal entryFee) {
        this.post = post;
        this.eventDate = eventDate;
        this.location = location;
        this.entryFee = entryFee != null ? entryFee : BigDecimal.ZERO;
    }

    // 도메인 메서드
    public void updateSchedule(LocalDateTime eventDate, String location, BigDecimal entryFee) {
        this.eventDate = eventDate;
        this.location = location;
        this.entryFee = entryFee;
    }

    public void updateSettlement(BigDecimal totalSpent, BigDecimal refundPerPerson) {
        this.totalSpent = totalSpent;
        this.refundPerPerson = refundPerPerson;
    }

    public void close() {
        this.status = "CLOSED";
        this.closedAt = LocalDateTime.now();
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

