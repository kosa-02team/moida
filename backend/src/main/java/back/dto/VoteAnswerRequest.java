package back.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record VoteAnswerRequest(
        @NotNull(message = "옵션 ID 리스트는 필수입니다")
        @NotEmpty(message = "최소 하나의 옵션을 선택해야 합니다")
        List<Long> optionIds  // 선택한 옵션 ID 리스트
) {
}
