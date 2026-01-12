package back.dto;

import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClubMemberRequest {
    private Long clubId;
    private Long userId;
    private String clubNickname;
}