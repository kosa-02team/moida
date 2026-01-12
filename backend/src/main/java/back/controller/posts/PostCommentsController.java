package back.controller.posts;

import back.dto.posts.comments.request.PostCommentRequest;
import back.dto.posts.comments.response.PostCommentsIdResponse;
import back.dto.posts.comments.response.PostCommentsResponse;
import back.service.posts.PostCommentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/club/{clubId}/posts/{postId}/comments")
public class PostCommentsController {

    private final PostCommentsService postCommentsService;

    @PostMapping
    public ResponseEntity<PostCommentsIdResponse> createComment(
            @RequestHeader(value = "X-DEV-USER-ID", required = false) Long devWriterId,
            @PathVariable Long clubId,
            @PathVariable Long postId,
            @RequestBody PostCommentRequest request) {
        PostCommentsIdResponse response = postCommentsService.createComment(devWriterId, clubId, postId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<PostCommentsIdResponse> updateComment(
            @RequestHeader(value = "X-DEV-USER-ID", required = false) Long devWriterId,
            @PathVariable Long clubId,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody PostCommentRequest request) {
        PostCommentsIdResponse response = postCommentsService.updateComment(devWriterId, clubId, postId, commentId,
                request);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PostCommentsResponse> getPostComments(
            @RequestHeader(value = "X-DEV-USER-ID", required = false) Long viewerId,
            @PathVariable Long clubId,
            @PathVariable Long postId,
            Pageable pageable) {
        PostCommentsResponse response = postCommentsService.getPostComments(viewerId, clubId, postId, pageable);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<PostCommentsIdResponse> deleteComment(
            @RequestHeader(value = "X-DEV-USER-ID", required = false) Long actorId,
            @PathVariable Long clubId,
            @PathVariable Long postId,
            @PathVariable Long commentId) {

        PostCommentsIdResponse response = postCommentsService.deleteComment(actorId, clubId, postId, commentId);
        return ResponseEntity.ok(response);
    }
}
