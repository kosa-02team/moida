package back.controller;

import back.common.response.SuccessResponse;
import back.dto.VoteAnswerRequest;
import back.dto.VoteCreateRequest;
import back.dto.VoteResponse;
import back.service.VoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @PostMapping("/{clubId}/votes")
    public SuccessResponse<VoteResponse> createVote(
            @PathVariable("clubId") Long clubId,
            // TODO: 실제 구현 시 @AuthenticationPrincipal 등으로 현재 로그인 유저 ID 주입
            @RequestBody VoteCreateRequest request
    ) {
        // 임시로 하드코딩, 나중에 인증 연동 시 교체
        Long currentUserId = 1L;
        VoteResponse response = voteService.createVote(clubId, currentUserId, request);
        return SuccessResponse.success(HttpStatus.OK, response);
    }

    @PostMapping("/{clubId}/votes/{voteId}/close")
    public SuccessResponse<Void> closeVote(
            @PathVariable("clubId") Long clubId,
            @PathVariable("voteId") Long voteId
            // TODO: 실제 구현 시 @AuthenticationPrincipal 등으로 현재 로그인 유저 ID 주입
    ) {
        // 임시로 하드코딩, 나중에 인증 연동 시 교체
        Long currentUserId = 1L;
        voteService.closeVote(clubId, voteId, currentUserId);
        return SuccessResponse.success(HttpStatus.OK);
    }

    @PostMapping("/{clubId}/votes/{voteId}/answers")
    public SuccessResponse<Void> answerVote(
            @PathVariable("clubId") Long clubId,
            @PathVariable("voteId") Long voteId,
            @RequestBody @Valid VoteAnswerRequest request
            // TODO: 실제 구현 시 @AuthenticationPrincipal 등으로 현재 로그인 유저 ID 주입
    ) {
        // 임시로 하드코딩, 나중에 인증 연동 시 교체
        Long currentUserId = 1L;
        voteService.answerVote(clubId, voteId, currentUserId, request);
        return SuccessResponse.success(HttpStatus.OK);
    }
}


