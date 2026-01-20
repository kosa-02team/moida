package back.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class VoteDeadlineEvent {
    private final Long voteId;
    private final String voteTitle;
    private final Long clubId; // 투표가 속한 모임 ID (Vote -> Schedule or Post -> Club) ... 복잡하지만 Vote 엔티티에 clubId 직접
                               // 연관이 없음.
    // Vote -> Schedule (있으면) -> Club
    // Vote -> Post (있으면) -> Club
    // 조회 시점에 clubId를 알아와서 넣어줘야 함.
}
