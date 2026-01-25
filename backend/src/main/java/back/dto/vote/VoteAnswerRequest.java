package back.dto.vote;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record VoteAnswerRequest(
        @NotNull(message = "옵션 ID 리스트는 필수입니다")
        // @NotEmpty 제거: 투표 취소를 위해 빈 배열 허용
        List<Long> optionIds  // 선택한 옵션 ID 리스트 (빈 배열이면 투표 취소)
) {
}
