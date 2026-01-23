package back.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ClubJoinRequestEvent {
    private final Long clubId;
    private final Long userId; // Applicant Users PK
    private final String applicantNickname;
    private final String clubName;
}
