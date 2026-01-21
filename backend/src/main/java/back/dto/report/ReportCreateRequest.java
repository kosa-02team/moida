package back.dto.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReportCreateRequest(
        @NotNull(message = "targetId는 필수입니다.")
        @Positive(message = "targetId는 양수여야 합니다.")
        Long targetId,
        
        @NotBlank(message = "reason은 필수입니다.")
        String reason,
        
        String photoUrl
) {}
