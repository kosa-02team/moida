package back.dto.schedule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ScheduleParticipantUpdateRequest(
        @NotBlank(message = "참석 상태는 필수입니다.")
        @Pattern(regexp = "ATTENDING|NOT_ATTENDING|UNDECIDED", message = "참석 상태는 ATTENDING, NOT_ATTENDING, UNDECIDED 중 하나여야 합니다.")
        String attendanceStatus
) {
}
