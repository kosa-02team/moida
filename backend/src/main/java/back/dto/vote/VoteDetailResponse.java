package back.dto.vote;

import java.time.LocalDateTime;
import java.util.List;

public record VoteDetailResponse(
        Long voteId,
        Long postId,
        String voteType,           // "GENERAL" 또는 "ATTENDANCE"
        Long scheduleId,
        Long creatorId,
        String title,
        String description,
        Boolean isAnonymous,
        Boolean allowMultiple,
        String status,             // "OPEN", "CLOSED"
        LocalDateTime deadline,
        LocalDateTime closedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<VoteOptionResponse> options,
        List<Long> mySelectedOptionIds  // 현재 사용자가 선택한 옵션 ID들
) {
}
