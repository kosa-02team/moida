package back.controller.post;

import back.config.security.UserPrincipal;
import back.dto.post.story.request.StoryUpdateRequest;
import back.dto.post.story.request.StoryCreateRequest;
import back.dto.post.story.response.AlbumCardResponse;
import back.dto.post.post.response.PostCardResponse;
import back.dto.post.story.response.PostDetailResponse;
import back.dto.post.post.response.PostIdResponse;
import back.exception.ClubException;
import back.service.post.PostLikeService;
import back.service.post.PostService;
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
public class PostController {

    private final PostService postService;
    private final PostLikeService postLikeService;

    @PostMapping
    public ResponseEntity<PostIdResponse> createStory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @RequestBody StoryCreateRequest request) {

        Long userId = requireUserId(principal);
        PostIdResponse response = postService.createStory(clubId, userId, request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailResponse> getPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @PathVariable Long postId){

        Long viewerId = (principal == null) ? null : principal.getUserId();

        return ResponseEntity.ok(postService.getPost(clubId, postId, viewerId));
    }

    @GetMapping("/albums/recent")
    public ResponseEntity<List<AlbumCardResponse>> getRecentAlbums(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @RequestParam(defaultValue = "2") int limit) {

        Long viewerId = (principal == null) ? null : principal.getUserId();

        return ResponseEntity.ok(postService.getRecentAlbums(clubId,viewerId, limit));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<PostCardResponse>> getRecentPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Long viewerId = (principal == null) ? null : principal.getUserId();

        return ResponseEntity.ok(postService.getRecentPosts(clubId, viewerId, pageable));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<PostIdResponse> updatePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @PathVariable Long postId,
            @RequestBody StoryUpdateRequest request) {

        Long userId = requireUserId(principal);

        PostIdResponse response = postService.updatePost(clubId, postId, userId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{postId}/blind")
    public ResponseEntity<Void> updatePostByAuthor(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @PathVariable Long postId){

        Long userId = requireUserId(principal);
        postService.blindPost(clubId, postId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @PathVariable Long postId){

        Long userId = requireUserId(principal);

        postService.deletePost(clubId, postId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/likes")
    public ResponseEntity<Void> likePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @PathVariable Long postId) {

        Long userId = requireUserId(principal);

        postLikeService.likePost(postId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{postId}/likes")
    public ResponseEntity<Void> unlikePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @PathVariable Long postId) {

        Long userId = requireUserId(principal);

        postLikeService.unlikePost(postId, userId);
        return ResponseEntity.ok().build();
    }

    private Long requireUserId(UserPrincipal principal) {
        if (principal == null) throw new ClubException.AuthLoginRequired();
        return principal.getUserId();
    }
}
