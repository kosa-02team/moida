package back.controller;

import back.common.response.SuccessResponse;
import back.config.security.UserPrincipal;
import back.domain.club.ClubMembers;
import back.domain.club.Clubs;
import back.dto.user.MyClubResponse;
import back.dto.user.UserResponse;
import back.dto.user.UserUpdateRequest;
import back.repository.club.ClubMemberRepository;
import back.repository.club.ClubRepository;
import back.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubRepository clubRepository;

    /**
     * 내 정보 조회
     * GET /api/users/me
     */
    @GetMapping("/me")
    public ResponseEntity<SuccessResponse<UserResponse>> getMyInfo(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal.getUserId();
        UserResponse response = userService.getUser(userId);
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK, response));
    }

    /**
     * 내 정보 수정
     * PUT /api/users/me
     */
    @PutMapping("/me")
    public ResponseEntity<SuccessResponse<UserResponse>> updateMyInfo(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UserUpdateRequest request) {
        Long userId = principal.getUserId();
        UserResponse response = userService.updateUser(userId, request);
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK, response));
    }

    /**
     * 내가 가입한 모임 목록 조회
     * GET /api/users/me/clubs
     */
    @GetMapping("/me/clubs")
    public ResponseEntity<SuccessResponse<List<MyClubResponse>>> getMyClubs(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal.getUserId();

        // ACTIVE 상태인 모임 멤버십 조회
        List<ClubMembers> memberships = clubMemberRepository.findByUserIdAndStatus(
                userId, ClubMembers.Status.ACTIVE);

        // ClubMembers에서 Club 정보와 함께 MyClubResponse로 변환
        List<MyClubResponse> responses = memberships.stream()
                .map(member -> {
                    Clubs club = clubRepository.findById(member.getClubId())
                            .orElse(null);
                    if (club == null) {
                        return null;
                    }
                    return MyClubResponse.builder()
                            .clubId(member.getClubId())
                            .name(club.getClubName())
                            .roles(List.of(member.getRole().name()))
                            .joinedAt(member.getJoinedAt() != null ? member.getJoinedAt().toString() : null)
                            .visibility(club.getVisibility() != null ? club.getVisibility().name() : null)
                            .status(club.getStatus() != null ? club.getStatus().name() : null)
                            .category(club.getCategory() != null ? club.getCategory().name() : null)
                            .coverImageUrl(club.getCoverImageUrl())
                            .build();
                })
                .filter(response -> response != null)
                .collect(Collectors.toList());

        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK, responses));
    }

    /**
     * 회원 탈퇴 (소프트 삭제)
     * DELETE /api/users/me
     */
    @DeleteMapping("/me")
    public ResponseEntity<SuccessResponse<Void>> deleteMyAccount(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal.getUserId();
        userService.deleteUser(userId);
        return ResponseEntity.ok(SuccessResponse.success(HttpStatus.OK, null));
    }
}
