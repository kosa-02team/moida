package back.service;

import back.domain.club.Clubs;
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
import back.repository.club.ClubRepository;
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
    private final ClubRepository clubRepository;

    public AdminDashboardResponse getDashboardStats() {
        long pendingReports = reportsRepository.countByStatus("PENDING");
        long bannedUsers = userRepository.countByStatus("BANNED");
        long totalUsers = userRepository.count();
        long totalClubs = clubRepository.count();
        long closedClubs = clubRepository.countByStatus(Clubs.Status.INACTIVE);

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
            clubPage = clubRepository.findByClubNameContaining(keyword, pageable);
        } else if (status != null && !status.isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            try {
                Clubs.Status clubStatus = Clubs.Status.valueOf(status.toUpperCase());
                clubPage = clubRepository.findByStatus(clubStatus, pageable);
            } catch (IllegalArgumentException e) {
                // If status string matches no enum, return empty or all?
                // Returning empty is safer. Or throw error.
                // Let's assume frontend sends correct status strings (ACTIVE, INACTIVE).
                // If "CLOSED" is sent, it won't match "INACTIVE".
                // I should handle "CLOSED" mapping if UI sends "CLOSED".
                if ("CLOSED".equalsIgnoreCase(status)) {
                    clubPage = clubRepository.findByStatus(Clubs.Status.INACTIVE, pageable);
                } else {
                    // Fallback to empty if invalid
                    // Or maybe findAll() if invalid? No, better error or empty.
                    // Let's try basic valueOf.
                    throw new AdminException(ErrorCode.INVALID_INPUT, "Invalid club status: " + status);
                }
            }
        } else {
            clubPage = clubRepository.findAll(pageable);
        }
        return clubPage.map(this::mapToClubResponse);
    }

    @Transactional
    public void manageClub(Long clubId, String action) {
        Clubs club = clubRepository.findById(clubId)
                .orElseThrow(() -> new AdminException(ErrorCode.CLUB_NOT_FOUND));

        if ("CLOSE".equalsIgnoreCase(action)) {
            club.close(); // Sets INACTIVE
        } else if ("ACTIVATE".equalsIgnoreCase(action)) {
            club.activate(); // Sets ACTIVE
        } else {
            throw new AdminException(ErrorCode.INVALID_INPUT, "잘못된 처리 작업입니다: " + action);
        }
    }

    private AdminReportResponse mapToReportResponse(Reports report) {
        String clubName = clubRepository.findById(report.getClubId())
                .map(Clubs::getClubName)
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
