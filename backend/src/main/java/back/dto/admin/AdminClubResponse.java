package back.dto.admin;

import back.domain.club.Clubs;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminClubResponse {
    private Long clubId;
    private String name;
    private Long ownerId;
    private String ownerName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;

    public static AdminClubResponse of(Clubs club, String ownerName) {
        return AdminClubResponse.builder()
                .clubId(club.getClubId())
                .name(club.getClubName()) // Changed from getName() to getClubName()
                .ownerId(club.getOwnerId())
                .ownerName(ownerName)
                .status(club.getStatus().name()) // Enum to String
                .createdAt(club.getCreatedAt())
                .closedAt(club.getClosedAt())
                .build();
    }
}
