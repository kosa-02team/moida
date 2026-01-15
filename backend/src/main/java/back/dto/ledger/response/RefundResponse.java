package back.dto.ledger.response;

import java.math.BigDecimal;

/**
 * 환급 응답
 */
public record RefundResponse(
        boolean success,
        String transferId, // 거래 고유 ID
        String message, // 메시지
        BigDecimal amount, // 환급 금액
        String recipientName, // 받는 사람 이름
        String recipientAccountMasked // 받는 사람 계좌 (마스킹)
) {
}
