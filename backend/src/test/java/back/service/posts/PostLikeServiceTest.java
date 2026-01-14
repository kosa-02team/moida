package back.service.posts;

import back.domain.posts.PostLikes;
import back.repository.posts.PostLikeRepository;
import back.repository.posts.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PostLikeServiceTest {

    @InjectMocks
    private PostLikeService postLikeService;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private PostRepository postRepository;

    @Test
    @DisplayName("게시글 좋아요 성공")
    void likePost_Success() {
        // given
        Long postId = 1L;
        Long userId = 100L;
        given(postLikeRepository.existsByPostIdAndUserId(postId, userId)).willReturn(false);
        given(postRepository.existsById(postId)).willReturn(true);

        // when
        postLikeService.likePost(postId, userId);

        // then
        verify(postLikeRepository).save(any(PostLikes.class));
    }

    @Test
    @DisplayName("이미 좋아요를 누른 경우 무시")
    void likePost_AlreadyLiked() {
        // given
        Long postId = 1L;
        Long userId = 100L;
        given(postLikeRepository.existsByPostIdAndUserId(postId, userId)).willReturn(true);

        // when
        postLikeService.likePost(postId, userId);

        // then
        verify(postLikeRepository, never()).save(any(PostLikes.class));
    }

    @Test
    @DisplayName("게시글이 존재하지 않는 경우 예외 발생")
    void likePost_PostNotFound() {
        // given
        Long postId = 1L;
        Long userId = 100L;
        given(postLikeRepository.existsByPostIdAndUserId(postId, userId)).willReturn(false);
        given(postRepository.existsById(postId)).willReturn(false);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> postLikeService.likePost(postId, userId));
    }

    @Test
    @DisplayName("게시글 좋아요 취소 성공")
    void unlikePost_Success() {
        // given
        Long postId = 1L;
        Long userId = 100L;

        // when
        postLikeService.unlikePost(postId, userId);

        // then
        verify(postLikeRepository).deleteByPostIdAndUserId(postId, userId);
    }
}
