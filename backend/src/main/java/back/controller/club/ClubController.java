package back.controller.club;

import back.common.response.SuccessResponse;
import back.config.security.UserPrincipal;
import back.domain.club.ClubMembers;
import back.domain.club.Clubs;
import back.dto.club.ClubMemberResponse;
import back.dto.club.ClubRequest;
import back.dto.club.ClubResponse;
import back.exception.ClubException;
import back.exception.response.ErrorCode;
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
import java.util.Locale;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;
    private final ClubMemberService clubMemberService;
    private final ClubAuthorization clubAuthorization;

    /**
     * enum 문자열을 안전하게 변환하는 공통 헬퍼 메서드
     * @param value 변환할 문자열
     * @param enumClass enum 클래스
     * @param errorCode 변환 실패 시 던질 에러 코드
     * @return 변환된 enum 값
     * @throws ClubException 유효하지 않은 enum 값일 경우
     */
    private <T extends Enum<T>> T parseEnum(String value, Class<T> enumClass, ErrorCode errorCode) {
        try {
            return Enum.valueOf(enumClass, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ClubException(errorCode);
        }
    }

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
        Clubs.Category categoryEnum = parseEnum(category, Clubs.Category.class, ErrorCode.CLUB_INVALID_CATEGORY);
        Page<ClubResponse> response = clubService.getClubsByCategory(categoryEnum, pageable);
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK, response));
    }

    // 카테고리 + 상태별 모임 조회
    @GetMapping("/category/{category}/status/{status}")
    public ResponseEntity<SuccessResponse<Page<ClubResponse>>> getClubsByCategoryAndStatus(
            @PathVariable String category,
            @PathVariable String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Clubs.Category categoryEnum = parseEnum(category, Clubs.Category.class, ErrorCode.CLUB_INVALID_CATEGORY);
        Clubs.Status statusEnum = parseEnum(status, Clubs.Status.class, ErrorCode.CLUB_INVALID_STATUS);
        Page<ClubResponse> response = clubService.getClubsByCategoryAndStatus(categoryEnum, statusEnum, pageable);
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
            Clubs.Category categoryEnum = parseEnum(category, Clubs.Category.class, ErrorCode.CLUB_INVALID_CATEGORY);
            response = clubService.searchClubsByCategoryAndName(categoryEnum, clubName, pageable);
        } else if (category != null) {
            // 카테고리만 검색
            Clubs.Category categoryEnum = parseEnum(category, Clubs.Category.class, ErrorCode.CLUB_INVALID_CATEGORY);
            response = clubService.getClubsByCategory(categoryEnum, pageable);
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
        ClubMembers.Status statusEnum;
        try {
            statusEnum = ClubMembers.Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ClubException(ErrorCode.CLUB_INVALID_STATUS);
        }
        
        List<ClubMemberResponse> members = clubMemberService.getMembers(clubId, statusEnum);
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
        
        ClubMembers.Role roleEnum;
        try {
            roleEnum = ClubMembers.Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ClubException(ErrorCode.CLUB_INVALID_ROLE);
        }
        
        ClubMemberResponse response = clubMemberService.updateMemberRole(clubId, memberId, roleEnum);
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK, response));
    }

    @Setter
    @Getter
    @NoArgsConstructor
    public static class RoleUpdateRequest {
        private String role;
    }
}
