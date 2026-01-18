package back.controller.club;

import back.common.response.SuccessResponse;
import back.config.security.UserPrincipal;
import back.dto.club.ClubRequest;
import back.dto.club.ClubResponse;
import back.service.club.ClubService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;
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
}
