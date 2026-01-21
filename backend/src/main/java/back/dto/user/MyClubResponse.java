package back.dto.user;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MyClubResponse {
    private Long clubId;
    private String name;
    private List<String> roles;
    private String joinedAt;
    private String visibility;
    private String status;
    private String category;
}
