package back.dto;

import back.domain.ClubMembers;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubMemberResponse {
    private Long memberId;
    private Long clubId;
    private Long userId;
    private String clubNickname;
    private List<String> roles;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime joinedAt;


    public static ClubMemberResponse from(ClubMembers entity) {
        return ClubMemberResponse.builder()
                .memberId(entity.getMemberId())
                .clubId(entity.getClubId())
                .userId(entity.getUserId())
                .clubNickname(entity.getClubNickname())
                .roles(entity.getRoles())
                .status(entity.getStatus().name())
                .createdAt(entity.getCreatedAt())
                .joinedAt(entity.getJoinedAt())
                .build();
    }
}