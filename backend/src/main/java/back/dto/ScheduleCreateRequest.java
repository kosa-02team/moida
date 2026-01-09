package back.dto;

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
        BigDecimal entryFee
) {
}
