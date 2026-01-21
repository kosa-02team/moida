package back.dto.schedule;

import java.math.BigDecimal;

/**
 * 일정 마무리(정산 완료) 요청 DTO
 */
public record ScheduleFinalizeRequest(
        BigDecimal totalSpent  // 총 지출 금액
) {
}
