package back.dto.schedule;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ScheduleCreateRequest(
        @NotBlank(message = "일정 이름은 필수입니다")
        String scheduleName,
        
        @NotNull(message = "시작일시는 필수입니다")
        LocalDateTime eventDate,
        
        @NotNull(message = "종료일시는 필수입니다")
        LocalDateTime endDate,
        
        String location,
        String description,
        BigDecimal entryFee,
        LocalDateTime voteDeadline  // 투표 종료 시간 (선택사항)
) {
    @AssertTrue(message = "종료일시는 시작일시보다 이후여야 합니다")
    public boolean isValidDateRange() {
        if (eventDate == null || endDate == null) {
            return true; // @NotNull이 처리하므로 여기서는 true 반환
        }
        return endDate.isAfter(eventDate);
    }
}
