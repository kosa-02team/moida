package back.controller.club;

import back.common.response.SuccessResponse;
import back.config.security.UserPrincipal;
import back.domain.club.Clubs;
import back.dto.club.ClubRequest;
import back.dto.club.ClubResponse;
import back.service.club.ClubService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
        Clubs.Category categoryEnum = Clubs.Category.valueOf(category.toUpperCase());
        Page<ClubResponse> response = clubService.getClubsByCategory(categoryEnum, pageable);
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK, response));
    }

    // 카테고리 + 상태별 모임 조회
    @GetMapping("/category/{category}/status/{status}")
    public ResponseEntity<SuccessResponse<Page<ClubResponse>>> getClubsByCategoryAndStatus(
            @PathVariable String category,
            @PathVariable String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Clubs.Category categoryEnum = Clubs.Category.valueOf(category.toUpperCase());
        Clubs.Status statusEnum = Clubs.Status.valueOf(status.toUpperCase());
        Page<ClubResponse> response = clubService.getClubsByCategoryAndStatus(categoryEnum, statusEnum, pageable);
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK, response));
    }

    // 카테고리 + 이름 검색
    @GetMapping("/search")
    public ResponseEntity<SuccessResponse<Page<ClubResponse>>> searchClubs(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String clubName,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        if (category != null && clubName != null) {
            Clubs.Category categoryEnum = Clubs.Category.valueOf(category.toUpperCase());
            Page<ClubResponse> response = clubService.searchClubsByCategoryAndName(categoryEnum, clubName, pageable);
            return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK, response));
        } else {
            Page<ClubResponse> response = clubService.getAllClubs(pageable);
            return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK, response));
        }
    }
}
