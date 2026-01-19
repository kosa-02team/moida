package back.service;

import back.domain.club.Clubs;
import back.domain.Reports;
import back.domain.Users;
import back.dto.admin.AdminClubResponse;
import back.dto.admin.AdminDashboardResponse;
import back.dto.admin.AdminReportResponse;
import back.dto.admin.AdminUserResponse;
import back.exception.AdminException;
import back.repository.ReportsRepository;
import back.repository.UserRepository;
import back.repository.club.ClubRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private ReportsRepository reportsRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClubRepository clubRepository;

    @InjectMocks
    private AdminService adminService;

    private static <T> T newEntity(Class<T> type) {
        try {
            var ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Reports report(Long id, String status) {
        Reports r = newEntity(Reports.class);
        ReflectionTestUtils.setField(r, "reportId", id);
        ReflectionTestUtils.setField(r, "status", status);
        ReflectionTestUtils.setField(r, "clubId", 1L);
        ReflectionTestUtils.setField(r, "reporterId", 2L);
        ReflectionTestUtils.setField(r, "targetId", 3L);
        return r;
    }

    private Users user(Long id, String status) {
        Users u = newEntity(Users.class);
        ReflectionTestUtils.setField(u, "userId", id);
        ReflectionTestUtils.setField(u, "status", status);
        ReflectionTestUtils.setField(u, "realName", "User" + id);
        return u;
    }

    private Clubs club(Long id, Clubs.Status status) {
        Clubs c = newEntity(Clubs.class);
        ReflectionTestUtils.setField(c, "clubId", id);
        ReflectionTestUtils.setField(c, "status", status);
        ReflectionTestUtils.setField(c, "ownerId", 4L);
        ReflectionTestUtils.setField(c, "clubName", "Club" + id);
        return c;
    }

    @Test
    @DisplayName("대시보드 통계 조회 성공")
    void get_dashboard_stats_success() {
        // given
        given(reportsRepository.countByStatus("PENDING")).willReturn(5L);
        given(userRepository.countByStatus("BANNED")).willReturn(3L);
        given(userRepository.count()).willReturn(100L);
        given(clubRepository.count()).willReturn(20L);
        given(clubRepository.countByStatus(Clubs.Status.INACTIVE)).willReturn(1L);

        // when
        AdminDashboardResponse result = adminService.getDashboardStats();

        // then
        assertThat(result.getPendingReports()).isEqualTo(5L);
        assertThat(result.getBannedUsers()).isEqualTo(3L);
        assertThat(result.getTotalUsers()).isEqualTo(100L);
        assertThat(result.getTotalClubs()).isEqualTo(20L);
        assertThat(result.getClosedClubs()).isEqualTo(1L);
    }

    @Nested
    @DisplayName("신고 관리")
    class ReportManagement {
        @Test
        @DisplayName("신고 목록 조회 성공")
        void get_reports_success() {
            // given
            Reports r1 = report(1L, "PENDING");
            given(reportsRepository.findAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of(r1)));
            given(clubRepository.findById(1L)).willReturn(Optional.of(club(1L, Clubs.Status.ACTIVE)));
            given(userRepository.findById(2L)).willReturn(Optional.of(user(2L, "ACTIVE")));
            given(userRepository.findById(3L)).willReturn(Optional.of(user(3L, "ACTIVE")));

            // when
            Page<AdminReportResponse> result = adminService.getReports(Pageable.unpaged(), null);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getReportId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("신고 승인 성공")
        void approve_report_success() {
            // given
            Reports r1 = report(1L, "PENDING");
            given(reportsRepository.findById(1L)).willReturn(Optional.of(r1));

            // when
            adminService.processReport(1L, "APPROVE");

            // then
            assertThat(r1.getStatus()).isEqualTo("APPROVED");
        }
    }

    @Nested
    @DisplayName("모임 관리")
    class ClubManagement {
        @Test
        @DisplayName("모임 목록 조회 성공")
        void get_clubs_success() {
            // given
            Clubs c1 = club(1L, Clubs.Status.ACTIVE);
            given(clubRepository.findAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of(c1)));
            given(userRepository.findById(4L)).willReturn(Optional.of(user(4L, "ACTIVE")));

            // when
            Page<AdminClubResponse> result = adminService.getClubs(Pageable.unpaged(), null, null);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getClubId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("모임 폐쇄 성공")
        void close_club_success() {
            // given
            Clubs c1 = club(1L, Clubs.Status.ACTIVE);
            given(clubRepository.findById(1L)).willReturn(Optional.of(c1));

            // when
            adminService.manageClub(1L, "CLOSE");

            // then
            assertThat(c1.getStatus()).isEqualTo(Clubs.Status.INACTIVE);
        }

        @Test
        @DisplayName("모임 활성화 성공")
        void activate_club_success() {
            // given
            Clubs c1 = club(1L, Clubs.Status.INACTIVE);
            given(clubRepository.findById(1L)).willReturn(Optional.of(c1));

            // when
            adminService.manageClub(1L, "ACTIVATE");

            // then
            assertThat(c1.getStatus()).isEqualTo(Clubs.Status.ACTIVE);
        }
    }

    @Nested
    @DisplayName("회원 관리")
    class UserManagement {
        @Test
        @DisplayName("회원 목록 조회 성공")
        void get_users_success() {
            // given
            Users u1 = user(1L, "ACTIVE");
            given(userRepository.findAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of(u1)));

            // when
            Page<AdminUserResponse> result = adminService.getUsers(Pageable.unpaged(), null, null);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getUserId()).isEqualTo(1L);
        }
    }
}
