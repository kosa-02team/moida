package back.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ClubJoinEvent {
    private final Long clubId;
    private final Long memberId; // ClubMembers PK
    private final Long userId; // Users PK
    private final String clubName;
}
