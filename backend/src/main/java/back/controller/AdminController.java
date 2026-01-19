package back.controller;

import back.common.response.SuccessResponse;
import back.dto.admin.AdminClubResponse;
import back.dto.admin.AdminDashboardResponse;
import back.dto.admin.AdminReportResponse;
import back.dto.admin.AdminUserResponse;
import back.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "System Admin", description = "시스템 관리자 API")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "대시보드 통계 조회")
    @GetMapping("/dashboard")
    public ResponseEntity<SuccessResponse<AdminDashboardResponse>> getDashboardStats() {
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK, adminService.getDashboardStats()));
    }

    @Operation(summary = "신고 목록 조회 (페이징, 상태 필터)")
    @GetMapping("/reports")
    public ResponseEntity<SuccessResponse<Page<AdminReportResponse>>> getReports(
            @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK, adminService.getReports(pageable, status)));
    }

    @Operation(summary = "신고 상세 조회")
    @GetMapping("/reports/{reportId}")
    public ResponseEntity<SuccessResponse<AdminReportResponse>> getReportDetail(@PathVariable Long reportId) {
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK, adminService.getReportDetail(reportId)));
    }

    @Operation(summary = "신고 처리 (승인/반려)")
    @PostMapping("/reports/{reportId}")
    public ResponseEntity<SuccessResponse<Void>> processReport(@PathVariable Long reportId,
            @RequestBody ActionRequest request) {
        adminService.processReport(reportId, request.getAction());
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK));
    }

    @Operation(summary = "회원 목록 조회 (페이징, 검색, 상태 필터)")
    @GetMapping("/users")
    public ResponseEntity<SuccessResponse<Page<AdminUserResponse>>> getUsers(
            @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ResponseEntity
                .ok(SuccessResponse.success(HttpStatus.OK, adminService.getUsers(pageable, keyword, status)));
    }

    @Operation(summary = "회원 상태 관리 (정지/활성)")
    @PostMapping("/users/{userId}/status")
    public ResponseEntity<SuccessResponse<Void>> manageUser(@PathVariable Long userId,
            @RequestBody ActionRequest request) {
        adminService.manageUser(userId, request.getAction());
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK));
    }

    @Operation(summary = "모임 목록 조회 (페이징, 검색, 상태 필터)")
    @GetMapping("/clubs")
    public ResponseEntity<SuccessResponse<Page<AdminClubResponse>>> getClubs(
            @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ResponseEntity
                .ok(SuccessResponse.success(HttpStatus.OK, adminService.getClubs(pageable, keyword, status)));
    }

    @Operation(summary = "모임 상태 관리 (폐쇄/활성)")
    @PostMapping("/clubs/{clubId}/status")
    public ResponseEntity<SuccessResponse<Void>> manageClub(@PathVariable Long clubId,
            @RequestBody ActionRequest request) {
        adminService.manageClub(clubId, request.getAction());
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK));
    }

    @Setter
    @Getter
    @NoArgsConstructor
    public static class ActionRequest {
        private String action;
    }
}
