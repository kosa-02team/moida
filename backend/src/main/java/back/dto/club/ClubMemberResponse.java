package back.dto.club;

import back.domain.club.ClubMembers;
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
    private String nickname; // 클럽 내 닉네임
    private String clubNickname; // 클럽 내 닉네임 (프론트엔드 호환성 - nickname과 동일)
    private String realName; // 사용자 실제 이름 (Users 테이블에서 조인)
    private String role; // 단일 역할 (하위 호환성)
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime joinedAt;
    
    // 역할 리스트 (프론트엔드 호환성을 위해)
    private List<String> roles;


    public static ClubMemberResponse from(ClubMembers entity) {
        return ClubMemberResponse.builder()
                .memberId(entity.getMemberId())
                .clubId(entity.getClubId())
                .userId(entity.getUserId())
                .nickname(entity.getNickname())
                .clubNickname(entity.getNickname()) // nickname과 동일
                .role(entity.getRole().name())
                .status(entity.getStatus().name())
                .createdAt(entity.getCreatedAt())
                .joinedAt(entity.getJoinedAt())
                .roles(List.of(entity.getRole().name()))
                .build();
    }
    
    public static ClubMemberResponse from(ClubMembers entity, String realName) {
        return ClubMemberResponse.builder()
                .memberId(entity.getMemberId())
                .clubId(entity.getClubId())
                .userId(entity.getUserId())
                .nickname(entity.getNickname())
                .clubNickname(entity.getNickname()) // nickname과 동일
                .realName(realName)
                .role(entity.getRole().name())
                .status(entity.getStatus().name())
                .createdAt(entity.getCreatedAt())
                .joinedAt(entity.getJoinedAt())
                .roles(List.of(entity.getRole().name()))
                .build();
    }
}
