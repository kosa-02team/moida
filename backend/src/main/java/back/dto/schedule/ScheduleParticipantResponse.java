package back.dto.schedule;

import java.time.LocalDateTime;

public record ScheduleParticipantResponse(
        Long participantId,
        Long scheduleId,
        Long userId,
        String userName,           // Users 테이블에서 조회
        String attendanceStatus,   // ATTENDING, NOT_ATTENDING, UNDECIDED
        String feeStatus,          // PENDING, PAID, REFUNDED
        Boolean isRefunded,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
