package back.dto.vote;

import java.time.LocalDateTime;

public record VoteListResponse(
        Long voteId,
        Long postId,
        String voteType,           // "GENERAL" 또는 "ATTENDANCE"
        Long scheduleId,
        String title,
        String status,             // "OPEN", "CLOSED"
        LocalDateTime deadline,
        LocalDateTime closedAt,
        LocalDateTime createdAt,
        long totalVoteCount        // 총 투표 수
) {
}
