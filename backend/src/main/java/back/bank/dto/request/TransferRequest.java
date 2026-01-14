package back.bank.dto.request;

import java.math.BigDecimal;

public record TransferRequest(
        String fromAccountNumber,
        String toBankCode,
        String toAccountNumber,
        String toOwnerName,     // 받는 분 실명 검증에 사용 가능
        BigDecimal amount,
        String memo,
        String idempotencyKey   // 중복송금 방지
) {
}
