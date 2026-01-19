package back.service;

import back.domain.Clubs;
import back.domain.Reports;
import back.domain.Users;
import back.dto.admin.AdminClubResponse;
import back.dto.admin.AdminDashboardResponse;
import back.dto.admin.AdminReportResponse;
import back.dto.admin.AdminUserResponse;
import back.exception.AdminException;
import back.exception.response.ErrorCode;
import back.repository.ReportsRepository;
import back.repository.UserRepository;
import back.repository.clubs.ClubsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final ReportsRepository reportsRepository;
    private final UserRepository userRepository;
    private final ClubsRepository clubsRepository;

    public AdminDashboardResponse getDashboardStats() {
        long pendingReports = reportsRepository.countByStatus("PENDING");
        long bannedUsers = userRepository.countByStatus("BANNED");
        long totalUsers = userRepository.count();
        long totalClubs = clubsRepository.count();
        long closedClubs = clubsRepository.countByStatus("CLOSED");

        return AdminDashboardResponse.builder()
                .pendingReports(pendingReports)
                .bannedUsers(bannedUsers)
                .totalUsers(totalUsers)
                .totalClubs(totalClubs)
                .closedClubs(closedClubs)
                .build();
    }

    public Page<AdminReportResponse> getReports(Pageable pageable, String status) {
        Page<Reports> reportPage;
        if (status != null && !status.isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            reportPage = reportsRepository.findByStatus(status, pageable);
        } else {
            reportPage = reportsRepository.findAll(pageable);
        }
        return reportPage.map(this::mapToReportResponse);
    }

    public AdminReportResponse getReportDetail(Long reportId) {
        Reports report = reportsRepository.findById(reportId)
                .orElseThrow(() -> new AdminException(ErrorCode.RESOURCE_NOT_FOUND));
        return mapToReportResponse(report);
    }

    @Transactional
    public void processReport(Long reportId, String action) {
        Reports report = reportsRepository.findById(reportId)
                .orElseThrow(() -> new AdminException(ErrorCode.RESOURCE_NOT_FOUND));

        if ("APPROVE".equalsIgnoreCase(action)) {
            report.approve();
            // TODO: 연동 로직 (예: 회원 경고 카운트 증가 등)
        } else if ("REJECT".equalsIgnoreCase(action)) {
            report.reject();
        } else {
            throw new AdminException(ErrorCode.INVALID_INPUT, "잘못된 처리 작업입니다: " + action);
        }
    }

    public Page<AdminUserResponse> getUsers(Pageable pageable, String keyword, String status) {
        Page<Users> userPage;
        if (keyword != null && !keyword.isEmpty()) {
            userPage = userRepository.findByRealNameContaining(keyword, pageable);
        } else if (status != null && !status.isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            userPage = userRepository.findByStatus(status, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }
        return userPage.map(AdminUserResponse::from);
    }

    @Transactional
    public void manageUser(Long userId, String action) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new AdminException(ErrorCode.USER_NOT_FOUND));

        if ("BAN".equalsIgnoreCase(action)) {
            user.ban();
        } else if ("ACTIVATE".equalsIgnoreCase(action)) {
            user.activate();
        } else {
            throw new AdminException(ErrorCode.INVALID_INPUT, "잘못된 처리 작업입니다: " + action);
        }
    }

    public Page<AdminClubResponse> getClubs(Pageable pageable, String keyword, String status) {
        Page<Clubs> clubPage;
        if (keyword != null && !keyword.isEmpty()) {
            clubPage = clubsRepository.findByNameContaining(keyword, pageable);
        } else if (status != null && !status.isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            clubPage = clubsRepository.findByStatus(status, pageable);
        } else {
            clubPage = clubsRepository.findAll(pageable);
        }
        return clubPage.map(this::mapToClubResponse);
    }

    @Transactional
    public void manageClub(Long clubId, String action) {
        Clubs club = clubsRepository.findById(clubId)
                .orElseThrow(() -> new AdminException(ErrorCode.CLUB_NOT_FOUND));

        if ("CLOSE".equalsIgnoreCase(action)) {
            club.close();
        } else if ("ACTIVATE".equalsIgnoreCase(action)) {
            club.activate();
        } else {
            throw new AdminException(ErrorCode.INVALID_INPUT, "잘못된 처리 작업입니다: " + action);
        }
    }

    private AdminReportResponse mapToReportResponse(Reports report) {
        String clubName = clubsRepository.findById(report.getClubId())
                .map(Clubs::getName)
                .orElse("Unknown Club");
        String reporterName = userRepository.findById(report.getReporterId())
                .map(Users::getRealName)
                .orElse("Unknown User");
        String targetName = userRepository.findById(report.getTargetId())
                .map(Users::getRealName)
                .orElse("Unknown User");

        return AdminReportResponse.of(report, clubName, reporterName, targetName);
    }

    private AdminClubResponse mapToClubResponse(Clubs club) {
        String ownerName = userRepository.findById(club.getOwnerId())
                .map(Users::getRealName)
                .orElse("Unknown Owner");

        return AdminClubResponse.of(club, ownerName);
    }
}
