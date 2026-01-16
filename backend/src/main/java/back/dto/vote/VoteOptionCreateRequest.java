package back.dto.vote;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 투표 옵션 생성 요청 DTO
 * GENERAL 타입 투표 생성 시 사용자가 입력한 옵션 정보를 담습니다.
 */
public record VoteOptionCreateRequest(
        @NotBlank(message = "옵션 텍스트는 필수입니다")
        String optionText,
        
        @NotNull(message = "옵션 순서는 필수입니다")
        Integer order,
        
        LocalDateTime eventDate,  // 선택: 일정 투표 옵션일 경우 사용
        String location           // 선택: 장소 투표 옵션일 경우 사용
) {
}
