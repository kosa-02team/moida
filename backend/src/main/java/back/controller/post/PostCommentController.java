package back.controller.post;

import back.config.security.UserPrincipal;
import back.dto.post.comment.request.PostCommentRequest;
import back.dto.post.comment.response.PostCommentsIdResponse;
import back.dto.post.comment.response.PostCommentsResponse;
import back.exception.ClubException;
import back.service.post.PostCommentService;
import back.service.post.CommentLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/club/{clubId}/posts/{postId}/comments")
public class PostCommentController {

    private final PostCommentService postCommentService;
    private final CommentLikeService commentLikeService;

    @PostMapping
    public ResponseEntity<PostCommentsIdResponse> createComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @PathVariable Long postId,
            @RequestBody PostCommentRequest request) {

        Long userId = requireUserId(principal);

        PostCommentsIdResponse response = postCommentService.createComment(userId, clubId, postId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<PostCommentsIdResponse> updateComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody PostCommentRequest request) {
        Long userId = requireUserId(principal);
        PostCommentsIdResponse response = postCommentService.updateComment(userId, clubId, postId, commentId,
                request);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PostCommentsResponse> getPostComments(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @PathVariable Long postId,
            Pageable pageable) {
        Long viewerId = (principal == null) ? null : principal.getUserId();

        PostCommentsResponse response = postCommentService.getPostComments(viewerId, clubId, postId, pageable);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<PostCommentsIdResponse> deleteComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        Long userId = requireUserId(principal);

        PostCommentsIdResponse response = postCommentService.deleteComment(userId, clubId, postId, commentId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{commentId}/likes")
    public ResponseEntity<Void> likeComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        Long userId = requireUserId(principal);
        commentLikeService.likeComment(commentId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{commentId}/likes")
    public ResponseEntity<Void> unlikeComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        Long userId = requireUserId(principal);
        commentLikeService.unlikeComment(commentId, userId);
        return ResponseEntity.ok().build();
    }

    private Long requireUserId(UserPrincipal principal) {
        if (principal == null) throw new ClubException.AuthLoginRequired();
        return principal.getUserId();
    }
}
