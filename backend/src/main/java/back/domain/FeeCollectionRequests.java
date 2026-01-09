package back.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "fee_collection_requests")
public class FeeCollectionRequests extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id")
    private Long settlementId;

    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 20)
    private String status = "OPEN";

    // 생성자
    public FeeCollectionRequests(Long scheduleId, BigDecimal totalAmount) {
        this.scheduleId = scheduleId;
        this.totalAmount = totalAmount;
    }

    // 도메인 메서드
    public void open() {
        this.status = "OPEN";
    }

    public void close() {
        this.status = "CLOSED";
    }

    public void pending() {
        this.status = "PENDING";
    }

    public void updateTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
