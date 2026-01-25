package back.dto.schedule;

import java.math.BigDecimal;

/**
 * 정산 미리보기 응답 DTO
 */
public record SettlementPreviewResponse(
        Integer paidCount,          // 납부 인원
        BigDecimal totalIncome,     // 총 수입
        BigDecimal totalSpent,      // 총 지출 (자동 계산)
        BigDecimal balance,         // 잔액
        BigDecimal refundPerPerson, // 1인당 환급액
        BigDecimal totalRefund      // 총 환급액
) {
}
