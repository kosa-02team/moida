package back.bank.dto.request;

import java.math.BigDecimal;

public record TransferRequest(
        String fromAccountNumber,
        String toBankCode,
        String toAccountNumber,
        BigDecimal amount,
        String memo,
        String idempotencyKey
) {
}
