package back.controller.posts;

import back.config.security.UserPrincipal;
import back.dto.posts.story.request.StoryUpdateRequest;
import back.dto.posts.story.request.StoryCreateRequest;
import back.dto.posts.story.response.AlbumCardResponse;
import back.dto.posts.posts.response.PostCardResponse;
import back.dto.posts.story.response.PostDetailResponse;
import back.dto.posts.posts.response.PostIdResponse;
import back.exception.ClubAuthException;
import back.service.posts.PostsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clubs/{clubId}/posts")
public class PostsController {

    private final PostsService postsService;
    private final back.service.posts.PostLikesService postLikesService;

    @PostMapping
    public ResponseEntity<PostIdResponse> createStory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @RequestBody StoryCreateRequest request) {

        Long userId = requireUserId(principal);

        PostIdResponse response = postsService.createStory(clubId, userId, request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailResponse> getPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @PathVariable Long postId){

        Long viewerId = (principal == null) ? null : principal.getUserId();

        return ResponseEntity.ok(postsService.getPost(clubId, postId, viewerId));
    }

    @GetMapping("/albums/recent")
    public ResponseEntity<List<AlbumCardResponse>> getRecentAlbums(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @RequestParam(defaultValue = "2") int limit) {

        Long viewerId = (principal == null) ? null : principal.getUserId();

        return ResponseEntity.ok(postsService.getRecentAlbums(clubId,viewerId, limit));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<PostCardResponse>> getRecentPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Long viewerId = (principal == null) ? null : principal.getUserId();

        return ResponseEntity.ok(postsService.getRecentPosts(clubId, viewerId, pageable));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<PostIdResponse> updatePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @PathVariable Long postId,
            @RequestBody StoryUpdateRequest request) {

        Long userId = requireUserId(principal);

        PostIdResponse response = postsService.updatePost(clubId, postId, userId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{postId}/blind")
    public ResponseEntity<Void> updatePostByAuthor(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @PathVariable Long postId){

        Long userId = requireUserId(principal);
        postsService.blindPost(clubId, postId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @PathVariable Long postId){

        Long userId = requireUserId(principal);

        postsService.deletePost(clubId, postId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/likes")
    public ResponseEntity<Void> likePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @PathVariable Long postId) {

        Long userId = requireUserId(principal);

        postLikesService.likePost(postId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{postId}/likes")
    public ResponseEntity<Void> unlikePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @PathVariable Long postId) {

        Long userId = requireUserId(principal);

        postLikesService.unlikePost(postId, userId);
        return ResponseEntity.ok().build();
    }

    private Long requireUserId(UserPrincipal principal) {
        if (principal == null) throw new ClubAuthException.LoginRequired();
        return principal.getUserId();
    }
}
