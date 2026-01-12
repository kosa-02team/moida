package back.controller;

import back.domain.ClubMembers;
import back.dto.ClubMemberRequest;
import back.dto.ClubMemberResponse;
import back.service.ClubMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/club-member")
@RequiredArgsConstructor
public class ClubMemberController {

    private final ClubMemberService clubMemberService;

    @PostMapping("/join")
    public ResponseEntity<ClubMemberResponse> join(@RequestBody ClubMemberRequest request) {
        ClubMemberResponse response = clubMemberService.joinClub(request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{memberId}/approve")
    @PreAuthorize("@clubSecurity.isOwner(#clubId)")
    public ResponseEntity<ClubMemberResponse> approve(
            @PathVariable Long memberId,
            @RequestParam Long clubId) {

        return ResponseEntity.ok(clubMemberService.approveClubMember(memberId));
    }

    @DeleteMapping("/{memberId}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long memberId) {
        clubMemberService.rejectClubMember(memberId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{memberId}/kick")
    public ResponseEntity<Void> kick(@PathVariable Long memberId) {
        clubMemberService.kickMember(memberId);
        return ResponseEntity.ok().build();
    }
}
