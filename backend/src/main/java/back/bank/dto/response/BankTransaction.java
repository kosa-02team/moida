package back.bank.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BankTransaction(
        String txId,
        LocalDateTime occurredAt,
        String type,            // "DEPOSIT" / "WITHDRAW" 등
        BigDecimal amount,
        BigDecimal balanceAfter,
        String printContent
) {
}
