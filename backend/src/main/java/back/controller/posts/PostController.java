package back.controller.posts;

import back.dto.posts.request.PostUpdateRequest;
import back.dto.posts.request.StoryCreateRequest;
import back.dto.posts.response.PostResponse;
import back.service.posts.PostsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clubs/{clubId}/posts")
public class PostController {

    private final PostsService postsService;

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
        Long storyId = postsService.createStory(devUserId, clubId, request);
        return ResponseEntity.created(URI.create("/api/clubs/{clubId}/posts/" + storyId)).build();
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(
            @PathVariable Long clubId,
            @PathVariable Long postId) {
        return ResponseEntity.ok(postsService.getPost(clubId, postId));
    }

    @GetMapping
    public ResponseEntity<List<PostResponse>> getAllPosts(
            @PathVariable Long clubId
    ) {
        return ResponseEntity.ok(postsService.getAllPosts(clubId));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<Void> updatePost(
            @PathVariable Long clubId,
            @PathVariable Long postId,
            @RequestBody PostUpdateRequest request) {

        postsService.updatePost(clubId, postId, request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{postId}/blind")
    public ResponseEntity<Void> updatePostByAuthor(@PathVariable Long postId) {
        postsService.blindPost(postId);
        return ResponseEntity.ok().build();
    }
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId) {
        postsService.deletePost(postId);
        return ResponseEntity.noContent().build();
    }

}
