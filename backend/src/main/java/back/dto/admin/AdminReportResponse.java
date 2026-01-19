package back.dto.admin;

import back.domain.Reports;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminReportResponse {
    private Long reportId;
    private Long clubId;
    private String clubName;
    private Long reporterId;
    private String reporterName;
    private Long targetId;
    private String targetName;
    private String reason;
    private String photoUrl;
    private String status;
    private LocalDateTime createdAt;

    public static AdminReportResponse of(Reports report, String clubName, String reporterName, String targetName) {
        return AdminReportResponse.builder()
                .reportId(report.getReportId())
                .clubId(report.getClubId())
                .clubName(clubName)
                .reporterId(report.getReporterId())
                .reporterName(reporterName)
                .targetId(report.getTargetId())
                .targetName(targetName)
                .reason(report.getReason())
                .photoUrl(report.getPhotoUrl())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
