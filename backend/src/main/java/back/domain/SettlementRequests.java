package back.domain;

import back.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementRequests extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id")
    private Long settlementId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 20)
    private String status = "PENDING";

    // 생성자
    public SettlementRequests(Long postId, BigDecimal totalAmount) {
        this.postId = postId;
        this.totalAmount = totalAmount;
    }

    // 도메인 메서드
    public void approve() {
        this.status = "APPROVED";
    }

    public void reject() {
        this.status = "REJECTED";
    }

    public void complete() {
        this.status = "COMPLETED";
    }

    public void updateTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}

