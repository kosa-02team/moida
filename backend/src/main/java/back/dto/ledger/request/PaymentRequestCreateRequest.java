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
            Integer expiresInDays // N일 후 만료 (선택)
    ) {
    }
}
