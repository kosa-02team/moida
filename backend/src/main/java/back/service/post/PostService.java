package back.service.post;

import back.domain.post.PostImages;
import back.domain.post.PostMemberTag;
import back.domain.post.Posts;
import back.dto.post.request.PostUpdateRequest;
import back.dto.post.request.StoryCreateRequest;
import back.dto.post.response.PostResponse;
import back.exception.PostException;
import back.repository.post.PostImagesRepository;
import back.repository.post.PostMemberTagRepository;
import back.repository.post.PostImagesRepository;
import back.repository.post.PostMemberTagRepository;
import back.repository.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostImagesRepository postImagesRepository;
    private final PostMemberTagRepository postMemberTagRepository;

    @Transactional
    public Long createStory(Long clubId, Long writerId, StoryCreateRequest request) {
        //todo : 권한 확인, ClubAuthorizationService 호출해서 해야할듯?
        Posts post = Posts.story(
                writerId,
                clubId,
                request.scheduleId(),
                request.content()
        );

        Posts savedPost = postRepository.save(post);
        post.updatePlace(request.place());

        saveImages(savedPost, request.imagesUrl());
        saveTaggedMembers(savedPost.getPostId(), request.taggedMemberIds());

        return savedPost.getPostId();
    }

    public PostResponse getPost(Long postId) {
        //todo : 모임 공개 방식 확인 필요
        Posts post = postRepository.findById(postId)
                .orElseThrow(PostException.NotFound::new);

        return PostResponse.from(post);
    }

    public List<PostResponse> getAllPosts() {
        //todo : 모임 공개 방식 확인 필요
        return postRepository.findAll().stream()
                .filter(post -> post.getDeletedAt() == null) // Filter soft-deleted posts
                .map(PostResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updatePost(Long postId, PostUpdateRequest request) {
        //todo : 1. 수정 권한 확인
        // 2. 작성자가 같은지 확인,
        Posts post = postRepository.findById(postId)
                .orElseThrow(PostException.NotFound::new);

        if (post.getDeletedAt() != null) {
            throw new PostException.Deleted();
        }

        Long actorId=1L;
        if (!post.getWriterId().equals(actorId)) {
            throw new PostException.Forbidden();
        }

        post.updatePost(request.title(), request.content());

    }

    @Transactional
    public void deletePost(Long postId) {
        Posts post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with id: " + postId));
        post.delete();
    }

    @Transactional
    public void replaceImages(Long postId, List<String> urls) {
        Posts post = postRepository.findById(postId).orElseThrow();

        postImagesRepository.deleteByPost_PostId(postId);

        if (urls != null && !urls.isEmpty()) {
            postImagesRepository.saveAll(
                    urls.stream().map(url -> PostImages.of(post, url)).toList()
            );
        }
    }

    private void saveImages(Posts posts, List<String> imagesUrl){
        if (imagesUrl != null && !imagesUrl.isEmpty()) {
            List<PostImages> images = imagesUrl.stream()
                    .map(url -> PostImages.of(posts, url))
                    .toList();

            postImagesRepository.saveAll(images);
        }
    }

    private void saveTaggedMembers(Long postId, List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) return;

        List<PostMemberTag> tags = memberIds.stream()
                .map(memberId -> PostMemberTag.of(postId, memberId))
                .toList();
        postMemberTagRepository.saveAll(tags);
    }
}
