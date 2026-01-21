package back.domain.ledger;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "member_name", nullable = false, length = 50)
    private String memberName;

    @Column(name = "request_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RequestType requestType;

    @Column(name = "expected_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal expectedAmount;

    @Column(name = "expected_date", nullable = false)
    private LocalDate expectedDate;

    @Column(name = "match_days_range")
    private Integer matchDaysRange = 10; // 기본값 ±10일

    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.PENDING;

    @Column(name = "match_type", length = 20)
    @Enumerated(EnumType.STRING)
    private MatchType matchType;

    @Column(name = "matched_history_id")
    private Long matchedHistoryId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "matched_at")
    private LocalDateTime matchedAt;

    @Column(name = "matched_by")
    private Long matchedBy;

    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "billing_period", length = 20)
    private String billingPeriod;

    // 생성자
    public PaymentRequest(Long clubId, Long memberId, String memberName,
            RequestType requestType, BigDecimal expectedAmount,
            LocalDate expectedDate, Integer matchDaysRange,
            LocalDateTime expiresAt,
            Long scheduleId, String billingPeriod) { // 추가된 파라미터
        this.clubId = clubId;
        this.memberId = memberId;
        this.memberName = memberName;
        this.requestType = requestType;
        this.expectedAmount = expectedAmount;
        this.expectedDate = expectedDate;
        this.matchDaysRange = matchDaysRange != null ? matchDaysRange : 10;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
        this.status = RequestStatus.PENDING;

        // 추가된 매핑
        this.scheduleId = scheduleId;
        this.billingPeriod = billingPeriod;
    }

    // 도메인 메서드

    /**
     * 자동 매칭 처리
     */
    public void autoMatch(Long historyId) {
        this.status = RequestStatus.MATCHED;
        this.matchType = MatchType.AUTO_MATCHED;
        this.matchedHistoryId = historyId;
        this.matchedAt = LocalDateTime.now();
    }

    /**
     * 수동 매칭 처리 (외부 거래내역 연결)
     */
    public void confirmMatch(Long historyId, Long matchedBy) {
        this.status = RequestStatus.MATCHED;
        this.matchType = MatchType.CONFIRMED;
        this.matchedHistoryId = historyId;
        this.matchedAt = LocalDateTime.now();
        this.matchedBy = matchedBy;
    }

    /**
     * 수동 확인 처리 (현금 납부 등 거래내역 없음)
     */
    public void confirmManualCashPayment(Long matchedBy) {
        this.status = RequestStatus.MATCHED;
        this.matchType = MatchType.MANUAL_CASH;
        this.matchedAt = LocalDateTime.now();
        this.matchedBy = matchedBy;
    }

    /**
     * 만료 처리
     */
    public void expire() {
        this.status = RequestStatus.EXPIRED;
    }

    /**
     * 매칭 가능 여부 확인
     * - 자동 매칭은 PENDING만 가능
     * - 수동 매칭은 EXPIRED도 가능하도록 서비스에서 처리
     */
    public boolean isMatchable() {
        return this.status == RequestStatus.PENDING || this.status == RequestStatus.EXPIRED;    }

    /**
     + * 수동 매칭 가능 여부 (만료된 요청도 포함)
     + */
    public boolean isManualMatchable() {
        return this.status == RequestStatus.PENDING || this.status == RequestStatus.EXPIRED;
    }
    // Enum 정의

    public enum RequestType {
        MEMBERSHIP_FEE, // 회비
        SETTLEMENT, // 정산
        DEPOSIT // 입금
    }

    public enum RequestStatus {
        PENDING, // 대기중
        MATCHED, // 매칭완료
        EXPIRED // 만료
    }

    public enum MatchType {
        AUTO_MATCHED, // 자동 매칭
        CONFIRMED, // 수동 확인 (거래내역 연결)
        MANUAL_CASH // 수동 확인 (현금 직접 수령)
    }
}
