package back.controller.ledger;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionLogResponse {
    private Long transactionId;
    private Long clubId;
    private Long scheduleId;
    private Long accountId;
    private String type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String description;
    private Long editorId;
    private LocalDateTime createdAt;
    private Long bankHistoryId;
    private String matchedMemberName;
}
