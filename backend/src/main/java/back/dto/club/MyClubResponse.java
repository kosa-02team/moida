package back.dto.club;

import back.domain.club.ClubMembers;
import back.domain.club.Clubs;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class MyClubResponse {
    private Long clubId;
    private String name;
    private List<String> roles;
    private LocalDateTime joinedAt;
    private String visibility;
    private String status;
    private String category;
    private String coverImageUrl;

    public static MyClubResponse from(Clubs club, ClubMembers member) {
        return MyClubResponse.builder()
                .clubId(club.getClubId())
                .name(club.getClubName())
                .roles(List.of(member.getRole().name()))
                .joinedAt(member.getJoinedAt())
                .visibility(club.getVisibility().name())
                .status(club.getStatus().name())
                .category(club.getCategory() != null ? club.getCategory().name() : null)
                .coverImageUrl(club.getCoverImageUrl())
                .build();
    }
}
