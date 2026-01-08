package back.service.post;

import back.domain.post.Posts;
import back.dto.post.request.PostUpdateRequest;
import back.dto.post.response.PostResponse;
import back.repository.post.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class PostServiceTests {

    @InjectMocks
    private PostService postService;

    @Mock
    private PostRepository postRepository;

    @Nested
    @DisplayName("모임 게시글 전체 조회")
    class ListPosts { /* 목록 */

        @Test
        @DisplayName("[GUEST] 공개 모임 게시글 조회 성공")
        void list_club_public_posts_public() {
            // given
            Posts post1 = new Posts(1L, 1L, "NOTICE", "Title 1", "Content 1");
            Posts post2 = new Posts(1L, 1L, "FREE", "Title 2", "Content 2");
            ReflectionTestUtils.setField(post1, "postId", 1L);
            ReflectionTestUtils.setField(post2, "postId", 2L);

            given(postRepository.findAll()).willReturn(List.of(post1, post2));

            // when
            List<PostResponse> result = postService.getAllPosts();

            // then
            assertThat(result).hasSize(2);
            then(postRepository).should(times(1)).findAll();
        }

        @Test
        @DisplayName("[GUEST] 공개 모임 게시글 조회 성공 - 비공개 게시글")
        void list_club_public_posts_private() {

        }

        @Test
        @DisplayName("[MEMBER] 비공개 모임 게시글 조회 성공")
        void list_club_private() {
        }
    }

    @Nested
    class GetPostDetail { /* 상세 조회 */

        @Test
        @DisplayName("[GUEST] 공개 모임 게시글 상세 조회 성공")
        void get_post_public() {
            // given
            Long postId = 1L;
            Posts post = new Posts(1L, 1L, "NOTICE", "Title", "Content");
            ReflectionTestUtils.setField(post, "postId", postId);

            given(postRepository.findById(postId)).willReturn(Optional.of(post));

            // when
            PostResponse response = postService.getPost(postId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.postId()).isEqualTo(postId);
            then(postRepository).should(times(1)).findById(postId);
        }

        @Test
        @DisplayName("[GUEST] 게시글 상세 조회 실패 - 없는 게시글")
        void get_no_post_guest() {
            // given
            Long postId = 999L;
            given(postRepository.findById(postId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postService.getPost(postId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Post not found");
        }

        @Test
        @DisplayName("[GUEST] 공개 모임 게시글 상세 조회 실패 - 비공개 게시글")
        void get_post_private_guest() {
        }

        @Test
        @DisplayName("[MEMBER] 비공개 모임 게시글 상세 조회 성공")
        void get_post_private_member() {
        }

        @Nested
        @DisplayName("게시글 내 댓글 전체 조회")
        class getComments {
        }

        @Nested
        @DisplayName("게시글 내 좋아요 수 조회")
        class getLikes {
        }
    }

    @Nested
    class CreatePost { /* 게시글 생성 */

        @Test
        @DisplayName("[MEMBER] 모임 게시글 생성 성공")
        void create_post_member() {
            // given
            GeneralPostCreateRequest request = new GeneralPostCreateRequest(1L, 1L, "NOTICE", "New Title", "New Content");
            Posts savedPost = new Posts(1L, 1L, "NOTICE", "New Title", "New Content");
            ReflectionTestUtils.setField(savedPost, "postId", 1L);

            given(postRepository.save(any(Posts.class))).willReturn(savedPost);

            // when
            Long postId = postService.createPost(request);

            // then
            assertThat(postId).isEqualTo(1L);
            then(postRepository).should(times(1)).save(any(Posts.class));
        }

        @Test
        @DisplayName("[GUEST] 모임 게시글 생성 실패 - 게시글 생성 권한 없음")
        void create_post_guest() {

        }

        @Nested
        class CreateComment {
        }

        @Nested
        class CreateLike {
        }
    }

    @Nested
    class UpdatePost { /* 수정 */

        @Test
        @DisplayName("[MEMBER] 모임 게시글 수정 성공")
        void update_post_member() {
            // given
            Long postId = 1L;
            PostUpdateRequest request = new PostUpdateRequest("Updated Title", "Updated Content");
            Posts post = new Posts(1L, 1L, "NOTICE", "Title", "Content");
            ReflectionTestUtils.setField(post, "postId", postId);

            given(postRepository.findById(postId)).willReturn(Optional.of(post));

            // when
            postService.updatePost(postId, request);

            // then
            assertThat(post.getTitle()).isEqualTo("Updated Title");
            assertThat(post.getContent()).isEqualTo("Updated Content");
        }

        @Test
        @DisplayName("[ADMIN] 모임 게시글 수정 성공")
        void update_post_admin_ban() {
        }

        @Test
        @DisplayName("[ADMIN] 모임 게시글 수정 실패 - 게시글 수정 권한 없음")
        void update_post_admin() {
        }

        @Test
        @DisplayName("[MEMBER] 모임 게시글 수정 실패 - 게시글 수정 권한 없음, 작성자 아님")
        void update_post_member_not_writer() {
        }

        @Nested
        class UpdateComment {
        }
    }

    @Nested
    class DeletePost { /* 삭제 */

        @Test
        @DisplayName("[MEMBER] 모임 게시글 삭제 성공")
        void delete_post_member() {
            // given
            Long postId = 1L;
            Posts post = new Posts(1L, 1L, "NOTICE", "Title", "Content");
            ReflectionTestUtils.setField(post, "postId", postId);

            given(postRepository.findById(postId)).willReturn(Optional.of(post));

            // when
            postService.deletePost(postId);

            // then
            assertThat(post.getDeletedAt()).isNotNull(); // Soft delete check
        }

        @Test
        @DisplayName("[MEMBER] 모임 게시글 삭제 실패 - 게시글 삭제 권한 없음, 작성자 아님")
        void delete_post_guest() {
        }

        @Nested
        class DeleteComment {
        }
    }
}
