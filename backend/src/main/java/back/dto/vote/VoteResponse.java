package back.dto.vote;

public record VoteResponse(
        Long voteId,
        Long postId,
        String voteType,      // "GENERAL" 또는 "ATTENDANCE"
        String title,
        String description,
        String status,        // "OPEN", "CLOSED" 등
        Long scheduleId
) {
}
