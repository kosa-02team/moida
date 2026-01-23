package back.controller.club;

import back.common.response.SuccessResponse;
import back.config.security.UserPrincipal;
import back.dto.club.ClubMemberResponse;
import back.dto.club.ClubRequest;
import back.dto.club.ClubResponse;
import back.service.club.ClubMemberService;
import back.service.club.ClubService;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;
    private final ClubMemberService clubMemberService;
    private final ClubAuthorization clubAuthorization;

    @PostMapping
    public ResponseEntity<SuccessResponse<ClubResponse>> createClub(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ClubRequest request) {
        Long ownerId = clubAuthorization.requireUserId(principal);
        ClubResponse response = clubService.createClub(request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuccessResponse.success(HttpStatus.CREATED, response));
    }

    @GetMapping("/{clubId}")
    public ResponseEntity<SuccessResponse<ClubResponse>> getClub(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId) {
        Long viewerId = principal != null ? principal.getUserId() : null;
        ClubResponse response = clubService.getClub(clubId, viewerId);
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK, response));
    }

    @PutMapping("/{clubId}")
    @PreAuthorize("@clubSecurity.isOwner(#clubId)")
    public ResponseEntity<SuccessResponse<ClubResponse>> updateClub(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @Valid @RequestBody ClubRequest request) {
        Long ownerId = clubAuthorization.requireOwner(clubId, principal);
        ClubResponse response = clubService.updateClub(clubId, request, ownerId);
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK, response));
    }

    @PatchMapping("/{clubId}/close")
    @PreAuthorize("@clubSecurity.isOwner(#clubId)")
    public ResponseEntity<SuccessResponse<Void>> closeClub(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId) {
        Long ownerId = clubAuthorization.requireOwner(clubId, principal);
        clubService.closeClub(clubId, ownerId);
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK));
    }

    @PatchMapping("/{clubId}/activate")
    @PreAuthorize("@clubSecurity.isOwner(#clubId)")
    public ResponseEntity<SuccessResponse<Void>> activateClub(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId) {
        Long ownerId = clubAuthorization.requireOwner(clubId, principal);
        clubService.activateClub(clubId, ownerId);
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK));
    }

    /**
     * 모임장 위임
     */
    @PatchMapping("/{clubId}/transfer-ownership")
    @PreAuthorize("@clubSecurity.isOwner(#clubId)")
    public ResponseEntity<SuccessResponse<Void>> transferOwnership(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @Valid @RequestBody back.dto.club.TransferOwnershipRequest request) {
        Long currentOwnerId = clubAuthorization.requireOwner(clubId, principal);
        clubService.transferOwnership(clubId, currentOwnerId, request.newOwnerMemberId());
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK));
    }

    // 모든 모임 조회 (페이징)
    @GetMapping
    public ResponseEntity<SuccessResponse<Page<ClubResponse>>> getAllClubs(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ClubResponse> response = clubService.getAllClubs(pageable);
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK, response));
    }

    // 카테고리별 모임 조회
    @GetMapping("/category/{category}")
    public ResponseEntity<SuccessResponse<Page<ClubResponse>>> getClubsByCategory(
            @PathVariable String category,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ClubResponse> response = clubService.getClubsByCategory(category, pageable);
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK, response));
    }

    // 카테고리 + 상태별 모임 조회
    @GetMapping("/category/{category}/status/{status}")
    public ResponseEntity<SuccessResponse<Page<ClubResponse>>> getClubsByCategoryAndStatus(
            @PathVariable String category,
            @PathVariable String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ClubResponse> response = clubService.getClubsByCategoryAndStatus(category, status, pageable);
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK, response));
    }

    // 카테고리 + 이름 검색
    @GetMapping("/search")
    public ResponseEntity<SuccessResponse<Page<ClubResponse>>> searchClubs(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String clubName,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<ClubResponse> response;
        
        if (category != null && clubName != null) {
            // 카테고리 + 이름 모두 검색
            response = clubService.searchClubsByCategoryAndName(category, clubName, pageable);
        } else if (category != null) {
            // 카테고리만 검색
            response = clubService.getClubsByCategory(category, pageable);
        } else if (clubName != null) {
            // 이름만 검색
            response = clubService.searchClubsByName(clubName, pageable);
        } else {
            // 전체 조회
            response = clubService.getAllClubs(pageable);
        }
        
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK, response));
    }

    // 초대 코드로 모임 조회
    @GetMapping("/by-invite-code/{inviteCode}")
    public ResponseEntity<SuccessResponse<ClubResponse>> getClubByInviteCode(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String inviteCode) {
        Long viewerId = principal != null ? principal.getUserId() : null;
        ClubResponse response = clubService.getClubByInviteCode(inviteCode, viewerId);
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK, response));
    }

    // 모임 멤버 목록 조회
    @GetMapping("/{clubId}/members")
    public ResponseEntity<SuccessResponse<List<ClubMemberResponse>>> getMembers(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @RequestParam(required = false, defaultValue = "ACTIVE") String status) {
        if (status == null || status.trim().isEmpty()) {
            status = "ACTIVE"; // 기본값
        }
        
        List<ClubMemberResponse> members = clubMemberService.getMembers(clubId, status);
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK, members));
    }

    // 멤버 역할 변경
    @PatchMapping("/{clubId}/members/{memberId}/role")
    @PreAuthorize("@clubSecurity.isOwner(#clubId)")
    public ResponseEntity<SuccessResponse<ClubMemberResponse>> updateMemberRole(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @PathVariable Long memberId,
            @Valid @RequestBody RoleUpdateRequest request) {
        clubAuthorization.requireOwner(clubId, principal);
        
        if (request.getRole() == null || request.getRole().trim().isEmpty()) {
            throw new back.exception.ClubException(back.exception.response.ErrorCode.CLUB_INVALID_ROLE);
        }
        
        ClubMemberResponse response = clubMemberService.updateMemberRole(clubId, memberId, request.getRole());
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK, response));
    }

    @Setter
    @Getter
    @NoArgsConstructor
    public static class RoleUpdateRequest {
        private String role;
    }
}
