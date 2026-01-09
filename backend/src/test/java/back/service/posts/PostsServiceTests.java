package back.service.posts;

import back.domain.posts.PostImages;
import back.domain.posts.PostMemberTags;
import back.domain.posts.Posts;
import back.dto.posts.request.StoryCreateRequest;
import back.repository.posts.PostImagesRepository;
import back.repository.posts.PostMemberTagsRepository;
import back.repository.posts.PostsRepository;
import back.service.clubs.ClubsAuthorizationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class PostsServiceTests {
    @Mock private ClubsAuthorizationService clubAuthorizationService;

    @InjectMocks
    private PostsService postsService;

    @Mock
    private PostsRepository postsRepository;

    @Mock
    private PostImagesRepository postImagesRepository;

    @Mock
    private PostMemberTagsRepository postMemberTagsRepository;

    @Nested
    @DisplayName("모임 게시글 전체 조회")
    class ListPosts { /* 목록 */

//        @Test
//        @DisplayName("[GUEST] 공개 모임 게시글 조회 성공")
//        void list_club_public_posts_public() {
//            // given
//            Posts post1 = new Posts(1L, 1L, PostCategory.GENERAL , "Title 1", "Content 1");
//            Posts post2 = new Posts(1L, 1L, PostCategory.GENERAL,  "Title 2", "Content 2");
//            ReflectionTestUtils.setField(post1, "postId", 1L);
//            ReflectionTestUtils.setField(post2, "postId", 2L);
//
//            given(postRepository.findAll()).willReturn(List.of(post1, post2));
//
//            // when
//            List<PostResponse> result = postService.getAllPosts();
//
//            // then
//            assertThat(result).hasSize(2);
//            then(postRepository).should(times(1)).findAll();
//        }

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

//        @Test
//        @DisplayName("[GUEST] 공개 모임 게시글 상세 조회 성공")
//        void get_post_public() {
//            // given
//            Long postId = 1L;
//            Posts post = new Posts(1L, 1L, "NOTICE", "Title", "Content");
//            ReflectionTestUtils.setField(post, "postId", postId);
//
//            given(postRepository.findById(postId)).willReturn(Optional.of(post));
//
//            // when
//            PostResponse response = postService.getPost(postId);
//
//            // then
//            assertThat(response).isNotNull();
//            assertThat(response.postId()).isEqualTo(postId);
//            then(postRepository).should(times(1)).findById(postId);
//        }

//        @Test
//        @DisplayName("[GUEST] 게시글 상세 조회 실패 - 없는 게시글")
//        void get_no_post_guest() {
//            // given
//            Long postId = 999L;
//            given(postRepository.findById(postId)).willReturn(Optional.empty());
//
//            // when & then
//            assertThatThrownBy(() -> postService.getPost(clubId, postId))
//                    .isInstanceOf(IllegalArgumentException.class)
//                    .hasMessageContaining("Post not found");
//        }

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
        void create_post_member_success_without_images_tags() {
            // given
            Long clubId = 1L;
            Long writerId = 1L;

            StoryCreateRequest request = new StoryCreateRequest(
                    null,                       // scheduleId
                    "모임 게시글 생성",            // content
                    null,                       // imagesUrl
                    "  남한산성  ",                // place (trim 확인용)
                    null                        // taggedMemberIds
            );

            Posts savedPost = Posts.story(writerId, clubId, request.scheduleId(), request.content());
            ReflectionTestUtils.setField(savedPost, "postId", 10L);

            given(postsRepository.save(any(Posts.class))).willReturn(savedPost);

            // when
            Long postId = postsService.createStory(clubId, writerId, request);

            // then
            assertThat(postId).isEqualTo(10L);
            then(postsRepository).should(times(1)).save(any(Posts.class));
            then(postImagesRepository).shouldHaveNoInteractions();
            then(postMemberTagsRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("[MEMBER] 모임 게시글 생성 성공 - 이미지/태그 저장 포함")
        void create_post_member_success_with_images_tags() {
            // given
            Long clubId = 1L;
            Long writerId = 1L;

            StoryCreateRequest request = new StoryCreateRequest(
                    1L, // scheduleId -> SCHEDULE category
                    "모임 게시글 생성",
                    List.of("https://example.com/1.png", "https://example.com/2.png"),
                    "강남역",
                    List.of(2L, 3L)
            );

            Posts savedPost = Posts.story(writerId, clubId, request.scheduleId(), request.content());
            ReflectionTestUtils.setField(savedPost, "postId", 1L);

            given(postsRepository.save(any(Posts.class))).willReturn(savedPost);

            // when
            Long postId = postsService.createStory(clubId, writerId, request);

            // then
            assertThat(postId).isEqualTo(1L);

            then(postsRepository).should(times(1)).save(any(Posts.class));

            then(postImagesRepository).should(times(1)).saveAll(argThat((Iterable<PostImages> it) -> {
                List<PostImages> list = StreamSupport.stream(it.spliterator(), false)
                        .toList();

                return list.size() == 2
                        && list.get(0).getImageUrl().equals("https://example.com/1.png")
                        && list.get(1).getImageUrl().equals("https://example.com/2.png");
            }));

            then(postMemberTagsRepository).should(times(1)).saveAll(argThat((Iterable<PostMemberTags> it) -> {
                List<PostMemberTags> list = StreamSupport.stream(it.spliterator(), false)
                        .toList();

                return list.size() == 2
                        && list.stream().allMatch(tag -> tag.getPostId().equals(1L))
                        && list.stream().map(PostMemberTags::getMemberId).collect(Collectors.toSet())
                        .containsAll(java.util.Set.of(2L, 3L));
            }));
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

//        @Test
//        @DisplayName("[MEMBER] 모임 게시글 수정 성공")
//        void update_post_member() {
//            // given
//            Long postId = 1L;
//            PostUpdateRequest request = new PostUpdateRequest("Updated Title", "Updated Content");
//            Posts post = new Posts(1L, 1L, "NOTICE", "Title", "Content");
//            ReflectionTestUtils.setField(post, "postId", postId);
//
//            given(postRepository.findById(postId)).willReturn(Optional.of(post));
//
//            // when
//            postService.updatePost(postId, request);
//
//            // then
//            assertThat(post.getTitle()).isEqualTo("Updated Title");
//            assertThat(post.getContent()).isEqualTo("Updated Content");
//        }

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

//        @Test
//        @DisplayName("[MEMBER] 모임 게시글 삭제 성공")
//        void delete_post_member() {
//            // given
//            Long postId = 1L;
//            Posts post = new Posts(1L, 1L, "NOTICE", "Title", "Content");
//            ReflectionTestUtils.setField(post, "postId", postId);
//
//            given(postRepository.findById(postId)).willReturn(Optional.of(post));
//
//            // when
//            postService.deletePost(postId);
//
//            // then
//            assertThat(post.getDeletedAt()).isNotNull(); // Soft delete check
//        }

        @Test
        @DisplayName("[MEMBER] 모임 게시글 삭제 실패 - 게시글 삭제 권한 없음, 작성자 아님")
        void delete_post_guest() {
        }

        @Nested
        class DeleteComment {
        }
    }
}
