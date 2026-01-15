package back.dto.vote;

import java.time.LocalDateTime;

public record VoteOptionResponse(
        Long optionId,
        String optionText,
        Integer optionOrder,
        LocalDateTime eventDate,   // ATTENDANCE 타입에서 사용
        String location,           // ATTENDANCE 타입에서 사용
        Long voteCount             // 해당 옵션을 선택한 사람 수
) {
}
