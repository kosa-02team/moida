package back.dto.schedule;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ScheduleResponse(
        Long scheduleId,
        String scheduleName,
        LocalDateTime eventDate,
        LocalDateTime endDate,
        String location,
        String description,
        BigDecimal entryFee,
        BigDecimal totalSpent,
        BigDecimal refundPerPerson,
        BigDecimal collectedEntryFee, // 집계된 참가비 (입금된 금액)
        Integer paidParticipantsCount, // 참가비 납부한 인원 수
        String status,
        LocalDateTime closedAt,
        String cancelReason,
        LocalDateTime voteDeadline,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
