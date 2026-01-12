package back.controller;

import back.domain.ClubMembers;
import back.dto.ClubMemberRequest;
import back.dto.ClubMemberResponse;
import back.service.ClubMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/club-member")
@RequiredArgsConstructor
public class ClubMemberController {

    private final ClubMemberService clubMemberService;

    @PostMapping("/{clubId}/join")
    public ResponseEntity<ClubMemberResponse> joinClub(
            @PathVariable Long clubId,
            @Valid @RequestBody ClubMemberRequest request) {
        ClubMemberResponse response = clubMemberService.joinClub(clubId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{clubId}/members/{memberId}/approve")
    @PreAuthorize("@clubSecurity.isOwner(#clubId)")
    public ResponseEntity<ClubMemberResponse> approve(
            @PathVariable Long clubId,
            @PathVariable Long memberId) {
        return ResponseEntity.ok(clubMemberService.approveClubMember(clubId, memberId));
    }

    @PatchMapping("/{clubId}/members/{memberId}/reject")
    @PreAuthorize("@clubSecurity.isOwner(#clubId)")
    public ResponseEntity<Void> reject(
            @PathVariable Long clubId,
            @PathVariable Long memberId) {
        clubMemberService.rejectClubMember(clubId, memberId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{clubId}/members/{memberId}/kick")
    @PreAuthorize("@clubSecurity.isOwner(#clubId)")
    public ResponseEntity<Void> kick(
            @PathVariable Long clubId,
            @PathVariable Long memberId) {
        clubMemberService.kickMember(clubId, memberId);
        return ResponseEntity.ok().build();
    }
}
