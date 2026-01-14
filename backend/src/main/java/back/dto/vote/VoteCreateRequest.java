package back.dto.vote;

public record VoteCreateRequest(
        String voteType,      // "GENERAL" 또는 "ATTENDANCE"
        String title,
        String description,
        Boolean isAnonymous,
        Boolean allowMultiple,
        Long scheduleId       // ATTENDANCE 타입일 때만 사용, 없으면 null
) {
}
