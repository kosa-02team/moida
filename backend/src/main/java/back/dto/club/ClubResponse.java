package back.dto.club;

import back.domain.club.Clubs;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubResponse {

    private Long clubId;
    private String clubName;
    private Long ownerId;
    private String mainAccountId;
    private String inviteCode;
    private String status;
    private String visibility;
    private String type;
    private String category;
    private Integer maxMembers;
    private Integer currentMembers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;

    public static ClubResponse from(Clubs entity, Integer currentMembers) {
        return ClubResponse.builder()
                .clubId(entity.getClubId())
                .clubName(entity.getClubName())
                .ownerId(entity.getOwnerId())
                .mainAccountId(entity.getMainAccountId())
                .inviteCode(entity.getInviteCode())
                .status(entity.getStatus().name())
                .visibility(entity.getVisibility().name())
                .type(entity.getType().name())
                .category(entity.getCategory().name())
                .maxMembers(entity.getMaxMembers())
                .currentMembers(currentMembers)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .closedAt(entity.getClosedAt())
                .build();
    }

    public static ClubResponse limited(Clubs entity) {
        return ClubResponse.builder()
                .clubId(entity.getClubId())
                .clubName(entity.getClubName())
                .type(entity.getType().name())
                .category(entity.getCategory().name())
                .visibility(entity.getVisibility().name())
                .status(entity.getStatus().name())
                .build();
    }

    public static ClubResponse full(Clubs entity, Integer currentMembers) {
        return from(entity, currentMembers);
    }
}
