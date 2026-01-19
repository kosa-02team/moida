package back.dto.admin;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminDashboardResponse {
    private long pendingReports;
    private long bannedUsers;
    private long totalUsers;
    private long totalClubs;
    private long closedClubs;
}
