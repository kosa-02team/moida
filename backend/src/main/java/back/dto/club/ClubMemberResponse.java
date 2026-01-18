package back.dto.club;

import back.domain.club.ClubMembers;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubMemberResponse {
    private Long memberId;
    private Long clubId;
    private Long userId;
    private String nickname;
    private String role;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime joinedAt;


    public static ClubMemberResponse from(ClubMembers entity) {
        return ClubMemberResponse.builder()
                .memberId(entity.getMemberId())
                .clubId(entity.getClubId())
                .userId(entity.getUserId())
                .nickname(entity.getNickname())
                .role(entity.getRole().name())
                .status(entity.getStatus().name())
                .createdAt(entity.getCreatedAt())
                .joinedAt(entity.getJoinedAt())
                .build();
    }
}
