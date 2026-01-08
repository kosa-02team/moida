package back.controller.post;

import back.dto.post.request.PostUpdateRequest;
import back.dto.post.request.StoryCreateRequest;
import back.dto.post.response.PostResponse;
import back.service.post.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/club/{clubId}/posts")
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<Void> createStory(
            /* todo : security있으면
                RequestHeader말고 AuthenticationPrincipal로 변경예정
                @AuthenticationPrincipal UserPrincipal principal,
                principal.userId()로 접근
            */
            @RequestHeader(value = "X-DEV-USER-ID", required = false) Long devUserId,
            @PathVariable Long clubId,
            @RequestBody StoryCreateRequest request) {
        Long storyId = postService.createStory(devUserId, clubId, request);
        return ResponseEntity.created(URI.create("/api/club/{clubId}/posts/" + storyId)).build();
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.getPost(postId));
    }

    @GetMapping
    public ResponseEntity<List<PostResponse>> getAllPosts() {
        return ResponseEntity.ok(postService.getAllPosts());
    }

    @PutMapping("/{postId}")
    public ResponseEntity<Void> updatePost(@PathVariable Long postId, @RequestBody PostUpdateRequest request) {
        postService.updatePost(postId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId) {
        postService.deletePost(postId);
        return ResponseEntity.noContent().build();
    }
}
