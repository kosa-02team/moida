package back.dto.ledger.response;

import back.domain.ledger.PaymentRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 처리된 거래내역 응답 (오픈뱅킹 원본 + 매칭 정보)
 */
public record ProcessedTransactionResponse(
        // 거래내역 기본 정보
        Long historyId,
        String txId,
        LocalDateTime occurredAt,
        String type, // "DEPOSIT" or "WITHDRAW"
        BigDecimal amount,
        BigDecimal balanceAfter,
        String printContent, // 오픈뱅킹 print_content 원본

        // 매칭 정보 (우리 서비스 추가)
        MatchType matchType, // AUTO_MATCHED, UNMATCHED, CONFIRMED
        String matchedMemberName, // 매칭된 회원 이름
        Long matchedMemberId, // 매칭된 회원 ID
        String matchedRequestType, // "MEMBERSHIP_FEE", "SETTLEMENT" 등
        Long paymentRequestId // 매칭된 입금요청 ID
) {
    public enum MatchType {
        AUTO_MATCHED, // 자동 매칭
        UNMATCHED, // 미매칭
        CONFIRMED // 수동 확인
    }

    /**
     * 매칭되지 않은 거래내역 생성 (UNMATCHED)
     */
    public static ProcessedTransactionResponse unmatched(
            Long historyId, String txId, LocalDateTime occurredAt,
            String type, BigDecimal amount, BigDecimal balanceAfter, String printContent) {
        return new ProcessedTransactionResponse(
                historyId, txId, occurredAt, type, amount, balanceAfter, printContent,
                MatchType.UNMATCHED, null, null, null, null);
    }

    /**
     * 자동 매칭된 거래내역 생성 (AUTO_MATCHED)
     */
    public static ProcessedTransactionResponse autoMatched(
            Long historyId, String txId, LocalDateTime occurredAt,
            String type, BigDecimal amount, BigDecimal balanceAfter, String printContent,
            PaymentRequest request) {
        return new ProcessedTransactionResponse(
                historyId, txId, occurredAt, type, amount, balanceAfter, printContent,
                MatchType.AUTO_MATCHED,
                request.getMemberName(),
                request.getMemberId(),
                request.getRequestType().name(),
                request.getRequestId());
    }

    /**
     * 수동 확인된 거래내역 생성 (CONFIRMED)
     */
    public static ProcessedTransactionResponse confirmed(
            Long historyId, String txId, LocalDateTime occurredAt,
            String type, BigDecimal amount, BigDecimal balanceAfter, String printContent,
            PaymentRequest request) {
        return new ProcessedTransactionResponse(
                historyId, txId, occurredAt, type, amount, balanceAfter, printContent,
                MatchType.CONFIRMED,
                request.getMemberName(),
                request.getMemberId(),
                request.getRequestType().name(),
                request.getRequestId());
    }
}
