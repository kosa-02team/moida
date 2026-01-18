package back.dto.ledger.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 입금요청 생성 요청
 */
public record PaymentRequestCreateRequest(
        List<RequestItem> requests) {
    public record RequestItem(
            Long memberId,
            String memberName,
            String requestType, // "MEMBERSHIP_FEE", "SETTLEMENT", "DEPOSIT"
            BigDecimal expectedAmount,
            LocalDate expectedDate,
            Integer matchDaysRange, // ±N일 (선택, 기본 10일)
            Integer expiresInDays,   // N일 후 만료 (선택)

            //  [추가] 일정 관련 요청일 경우
            Long scheduleId,
            // [추가] 회비 관련 요청일 경우 (예: "2024-02")
            String billingPeriod
    ) {
    }
}
