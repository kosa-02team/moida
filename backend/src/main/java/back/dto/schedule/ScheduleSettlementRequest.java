package back.dto.schedule;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ScheduleSettlementRequest(
        @NotNull(message = "총 지출 금액은 필수입니다")
        @PositiveOrZero(message = "총 지출 금액은 0 이상이어야 합니다")
        BigDecimal totalSpent,

        @NotNull(message = "1인당 환급액은 필수입니다")
        @PositiveOrZero(message = "1인당 환급액은 0 이상이어야 합니다")
        BigDecimal refundPerPerson
) {
}
