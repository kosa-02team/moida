package back.controller.ledger;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ManualTransactionRequest(
        LocalDate occurredAt,
        String content,
        BigDecimal amount, // 수입(+), 지출(-)
        String type // "DEPOSIT" or "WITHDRAW"
) {
}
