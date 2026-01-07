package back.service.post;

import back.domain.post.Posts;
import back.dto.post.request.PostCreateRequest;
import back.dto.post.request.PostUpdateRequest;
import back.dto.post.response.PostResponse;
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

    @Transactional
    public Long createPost(PostCreateRequest request) {
        Posts post = new Posts(
                request.clubId(),
                request.writerId(),
                request.category(),
                request.title(),
                request.content());
        Posts savedPost = postRepository.save(post);
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
}
