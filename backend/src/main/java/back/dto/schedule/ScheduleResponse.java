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
        String status,
        LocalDateTime closedAt,
        String cancelReason,
        LocalDateTime voteDeadline,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
