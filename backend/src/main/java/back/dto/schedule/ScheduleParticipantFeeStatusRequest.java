package back.dto.schedule;

/**
 * 참가자 납부 상태 수정 요청 DTO
 */
public record ScheduleParticipantFeeStatusRequest(
        String feeStatus  // "PENDING", "PAID"
) {
}
