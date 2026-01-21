package back.dto.schedule;

/**
 * 참가자 환급 상태 수정 요청 DTO
 */
public record ScheduleParticipantRefundStatusRequest(
        Boolean isRefunded
) {
}
