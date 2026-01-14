package back.controller.vote;

import back.common.response.SuccessResponse;
import back.config.security.UserPrincipal;
import back.dto.vote.VoteAnswerRequest;
import back.dto.vote.VoteCreateRequest;
import back.dto.vote.VoteResponse;
import back.exception.ClubAuthException;
import back.service.vote.VoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @PostMapping("/{clubId}/votes")
    public SuccessResponse<VoteResponse> createVote(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("clubId") Long clubId,
            @RequestBody VoteCreateRequest request
    ) {
        Long currentUserId = requireUserId(principal);
        VoteResponse response = voteService.createVote(clubId, currentUserId, request);
        return SuccessResponse.success(HttpStatus.OK, response);
    }

    @PostMapping("/{clubId}/votes/{voteId}/close")
    public SuccessResponse<Void> closeVote(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("clubId") Long clubId,
            @PathVariable("voteId") Long voteId
    ) {
        Long currentUserId = requireUserId(principal);
        voteService.closeVote(clubId, voteId, currentUserId);
        return SuccessResponse.success(HttpStatus.OK);
    }

    @PostMapping("/{clubId}/votes/{voteId}/answers")
    public SuccessResponse<Void> answerVote(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("clubId") Long clubId,
            @PathVariable("voteId") Long voteId,
            @RequestBody @Valid VoteAnswerRequest request
    ) {
        Long currentUserId = requireUserId(principal);
        voteService.answerVote(clubId, voteId, currentUserId, request);
        return SuccessResponse.success(HttpStatus.OK);
    }

    private Long requireUserId(UserPrincipal principal) {
        if (principal == null) throw new ClubAuthException.LoginRequired();
        return principal.getUserId();
    }
}


