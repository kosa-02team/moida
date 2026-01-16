package back.dto.vote;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 투표 생성 요청 DTO
 */
public record VoteCreateRequest(
        String voteType,      // "GENERAL" 또는 "ATTENDANCE"
        String title,
        String description,
        Boolean isAnonymous,
        Boolean allowMultiple,
        Long scheduleId,       // ATTENDANCE 타입일 때만 사용, 없으면 null
        LocalDateTime deadline, // GENERAL 타입일 때 사용 (선택)
        @Valid List<VoteOptionCreateRequest> options  // GENERAL 타입일 때 사용 (사용자가 입력한 옵션들)
) {
}
