package back.controller.posts;

import back.dto.posts.request.StoryUpdateRequest;
import back.dto.posts.request.StoryCreateRequest;
import back.dto.posts.response.AlbumCardResponse;
import back.dto.posts.response.PostCardResponse;
import back.dto.posts.response.PostDetailResponse;
import back.dto.posts.response.PostIdResponse;
import back.service.posts.PostsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clubs/{clubId}/posts")
public class PostController {

    private final PostsService postsService;

    @PostMapping
    public ResponseEntity<PostIdResponse> createStory(
            /* todo : security있으면
                RequestHeader말고 AuthenticationPrincipal로 변경예정
                @AuthenticationPrincipal UserPrincipal principal,
                principal.userId()로 접근
            */
            @RequestHeader(value = "X-DEV-USER-ID", required = false) Long devUserId,
            @PathVariable Long clubId,
            @RequestBody StoryCreateRequest request) {

        PostIdResponse response = postsService.createStory(clubId,devUserId, request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailResponse> getPost(
            @PathVariable Long clubId,
            @PathVariable Long postId,
            @RequestHeader(value = "X-DEV-USER-ID", required = false) Long devViewerId
            ) {
        return ResponseEntity.ok(postsService.getPost(clubId, postId, devViewerId));
    }

    @GetMapping("/albums/recent")
    public ResponseEntity<List<AlbumCardResponse>> getRecentAlbums(
            @PathVariable Long clubId,
            @RequestHeader(value = "X-DEV-USER-ID", required = false) Long devViewerId,
            @RequestParam(defaultValue = "2") int limit
    ) {
        return ResponseEntity.ok(postsService.getRecentAlbums(clubId, devViewerId, limit));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<PostCardResponse>> getRecentPost(
            @PathVariable Long clubId,
            @RequestHeader(value = "X-DEV-USER-ID", required = false) Long devViewerId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable ) {

        return ResponseEntity.ok(postsService.getRecentPosts(clubId, devViewerId, pageable));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<PostIdResponse> updatePost(
            @PathVariable Long clubId,
            @PathVariable Long postId,
            @RequestHeader(value = "X-DEV-USER-ID", required = false) Long devWriterId,
            @RequestBody StoryUpdateRequest request) {

        PostIdResponse response = postsService.updatePost(clubId, postId, devWriterId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{postId}/blind")
    public ResponseEntity<Void> updatePostByAuthor(
            @PathVariable Long clubId,
            @PathVariable Long postId,
            @RequestHeader(value = "X-DEV-USER-ID", required = false) Long devWriterId
            ) {
        postsService.blindPost(clubId, postId, devWriterId);
        return ResponseEntity.ok().build();
    }
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long clubId,
            @PathVariable Long postId,
            @RequestHeader(value = "X-DEV-USER-ID", required = false) Long devWriterId
    ) {
        postsService.deletePost(clubId, postId, devWriterId);
        return ResponseEntity.noContent().build();
    }

}
