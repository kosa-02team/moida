package back.dto.ledger.request;

import java.math.BigDecimal;

/**
 * 모임 정산 환급 요청
 * - 모임장/총무가 남은 돈을 회원들에게 돌려줄 때 사용
 */
public record RefundRequest(
        Long clubId, // 모임 ID
        Long recipientUserId, // 받는 사람 (회원) ID
        String recipientName, // 받는 사람 이름
        String recipientBankCode, // 받는 사람 은행 코드
        String recipientAccountNum, // 받는 사람 계좌번호
        BigDecimal amount, // 환급 금액
        String memo // 메모 (예: "2026년 1월 정산 환급")
) {
}
