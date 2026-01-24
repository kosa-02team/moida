package back.dto.ledger.request;

import java.math.BigDecimal;

public record AdditionalFeeRequest(
        BigDecimal amountPerPerson,  // 1인당 추가 금액
        String reason                 // 추가 회비 사유 (선택)
) {}
