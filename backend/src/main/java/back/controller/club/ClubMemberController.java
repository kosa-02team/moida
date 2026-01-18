package back.controller.club;

import back.common.response.SuccessResponse;
import back.config.security.UserPrincipal;
import back.dto.club.ClubMemberRequest;
import back.dto.club.ClubMemberResponse;
import back.service.club.ClubMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/club-member")
@RequiredArgsConstructor
public class ClubMemberController {

    private final ClubMemberService clubMemberService;
    private final ClubAuthorization clubAuthorization;

    @PostMapping("/{clubId}/join")
    public ResponseEntity<SuccessResponse<ClubMemberResponse>> joinClub(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @Valid @RequestBody ClubMemberRequest request) {
        Long userId = clubAuthorization.requireUserId(principal);
        ClubMemberResponse response = clubMemberService.joinClub(clubId, userId, request);
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK, response));
    }

    @PatchMapping("/{clubId}/members/{memberId}/approve")
    @PreAuthorize("@clubSecurity.isOwner(#clubId)")
    public ResponseEntity<SuccessResponse<ClubMemberResponse>> approve(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @PathVariable Long memberId) {
        clubAuthorization.requireOwner(clubId, principal);
        ClubMemberResponse response = clubMemberService.approveClubMember(clubId, memberId);
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK, response));
    }

    @PatchMapping("/{clubId}/members/{memberId}/reject")
    @PreAuthorize("@clubSecurity.isOwner(#clubId)")
    public ResponseEntity<SuccessResponse<Void>> reject(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @PathVariable Long memberId) {
        clubAuthorization.requireOwner(clubId, principal);
        clubMemberService.rejectClubMember(clubId, memberId);
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK));
    }

    @PatchMapping("/{clubId}/members/{memberId}/kick")
    @PreAuthorize("@clubSecurity.isOwner(#clubId)")
    public ResponseEntity<SuccessResponse<Void>> kick(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @PathVariable Long memberId) {
        clubAuthorization.requireOwner(clubId, principal);
        clubMemberService.kickMember(clubId, memberId);
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK));
    }
}
