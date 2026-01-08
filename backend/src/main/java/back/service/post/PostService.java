package back.service.post;

import back.domain.post.PostImages;
import back.domain.post.PostMemberTag;
import back.domain.post.Posts;
import back.dto.post.request.PostUpdateRequest;
import back.dto.post.request.StoryCreateRequest;
import back.dto.post.response.PostResponse;
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
        //todo : 권한 확인, ClubService 호출해서 해야할듯?
        Posts post = Posts.story(
                writerId,
                clubId,
                request.scheduleId(),
                request.content()
        );

        Posts savedPost = postRepository.save(post);

        post.updatePlace(request.place());

        List<String> urls = request.imagesUrl();
        if (urls != null && !urls.isEmpty()) {
            List<PostImages> images = urls.stream()
                    .map(url -> PostImages.of(savedPost, url))
                    .toList();

            postImagesRepository.saveAll(images);
        }

        List<Long> memberIds = request.taggedMemberIds();
        if (memberIds != null && !memberIds.isEmpty()) {
            List<PostMemberTag> tags = memberIds.stream()
                    .map(memberId -> PostMemberTag.of(savedPost.getPostId(), memberId))
                    .toList();

            postMemberTagRepository.saveAll(tags);
        }

        return savedPost.getPostId();
    }

    public PostResponse getPost(Long postId) {
        Posts post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with id: " + postId));
        return PostResponse.from(post);
    }

    public List<PostResponse> getAllPosts() {
        return postRepository.findAll().stream()
                .filter(post -> post.getDeletedAt() == null) // Filter soft-deleted posts
                .map(PostResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updatePost(Long postId, PostUpdateRequest request) {
        Posts post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with id: " + postId));
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
}
