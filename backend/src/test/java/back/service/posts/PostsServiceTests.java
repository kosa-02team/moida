package back.service.posts;

import back.domain.Clubs;
import back.domain.Schedules;
import back.domain.Users;
import back.domain.posts.Comments;
import back.domain.posts.PostImages;
import back.domain.posts.Posts;
import back.dto.posts.comments.response.PostCommentsIdResponse;
import back.dto.posts.posts.response.PostCardBase;
import back.dto.posts.posts.response.PostCardResponse;
import back.dto.posts.story.response.PostDetailResponse;
import back.exception.ClubAuthException;
import back.exception.PostsException;
import back.repository.SchedulesRepository;
import back.repository.clubs.ClubsRepository;
import back.repository.posts.PostCommentsRepository;
import back.repository.posts.PostImagesRepository;
import back.repository.posts.PostMemberTagsRepository;
import back.repository.posts.PostsRepository;
import back.repository.users.UsersRepository;
import back.service.clubs.ClubsAuthorizationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class PostsServiceTests {
    @Mock private ClubsAuthorizationService clubAuthorizationService;

    @Mock private ClubsRepository clubsRepository;
    @Mock private UsersRepository usersRepository;
    @Mock private SchedulesRepository schedulesRepository;

    @Mock private PostsRepository postsRepository;
    @Mock private PostImagesRepository postImagesRepository;
    @Mock private PostMemberTagsRepository postMemberTagsRepository;

    @Mock private PostCommentsRepository postCommentsRepository;

    @InjectMocks
    private PostsService postsService;

    @InjectMocks
    private PostCommentsService postCommentsService;

    private static <T> T newEntity(Class<T> type) {
        try {
            var ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Clubs club(Long id) {
        Clubs c = newEntity(Clubs.class);
        ReflectionTestUtils.setField(c, "clubId", id);
        return c;
    }

    private Users user(Long id) {
        Users u = newEntity(Users.class);
        ReflectionTestUtils.setField(u, "userId", id);
        return u;
    }

    private Schedules schedule(Long id) {
        Schedules s = newEntity(Schedules.class);
        ReflectionTestUtils.setField(s, "scheduleId", id);
        return s;
    }

    @Nested
    @DisplayName("모임 게시글 전체 조회")
    class ListPosts { /* 목록 */

        @Test
        @DisplayName("[GUEST] 공개 모임 게시글 조회 성공")
        void list_club_public_posts_public() {
            // given
            Long clubId = 1L;
            Long viewerId = null; // 게스트면 null 쓰는 정책이면
            Pageable pageable = PageRequest.of(0, 10);

            // 권한 통과 스텁
            willDoNothing().given(clubAuthorizationService)
                    .validateAndGetClubForReadPosts(clubId, viewerId);

            // repository가 반환하는 projection mock
            PostCardBase p1 = mock(PostCardBase.class);
            given(p1.postId()).willReturn(1L);
            PostCardBase p2 = mock(PostCardBase.class);
            given(p2.postId()).willReturn(2L);

            given(postsRepository.findPostCards(eq(clubId), eq(pageable)))
                    .willReturn(new PageImpl<>(List.of(p1, p2), pageable, 2));

            // 이미지 조회 스텁
            PostImages img1 = mock(PostImages.class);
            given(img1.getPostId()).willReturn(1L);
            given(img1.getImageUrl()).willReturn("https://example.com/1.png");

            given(postImagesRepository.findByPostIdIn(List.of(1L, 2L)))
                    .willReturn(List.of(img1));

            // when
            List<PostCardResponse> result = postsService.getRecentPosts(clubId, viewerId, pageable);

            // then
            assertThat(result).hasSize(2);
            then(clubAuthorizationService).should().validateAndGetClubForReadPosts(clubId, viewerId);
            then(postsRepository).should().findPostCards(clubId, pageable);
            then(postImagesRepository).should().findByPostIdIn(List.of(1L, 2L));
        }

        @Test
        @DisplayName("[GUEST] 비공개 모임 게시글 조회 실패")
        void list_club_public_posts_private() {
            // given
            Long clubId = 1L;
            Long viewerId = null; // 게스트
            Pageable pageable = PageRequest.of(0, 10);

            willThrow(new ClubAuthException.LoginRequired()) // 프로젝트 예외로 교체
                    .given(clubAuthorizationService)
                    .validateAndGetClubForReadPosts(clubId, viewerId);

            // when & then
            assertThatThrownBy(() -> postsService.getRecentPosts(clubId, viewerId, pageable))
                    .isInstanceOf(ClubAuthException.class);

            then(postsRepository).shouldHaveNoInteractions();
            then(postImagesRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("[MEMBER] 비공개 모임 게시글 조회 성공")
        void list_club_private() {
            // given
            Long clubId = 1L;
            Long viewerId = 10L; // 멤버
            Pageable pageable = PageRequest.of(0, 10);

            willDoNothing().given(clubAuthorizationService)
                    .validateAndGetClubForReadPosts(clubId, viewerId);

            PostCardBase p1 = mock(PostCardBase.class);
            given(p1.postId()).willReturn(1L);

            given(postsRepository.findPostCards(eq(clubId), eq(pageable)))
                    .willReturn(new PageImpl<>(List.of(p1), pageable, 1));

            given(postImagesRepository.findByPostIdIn(List.of(1L)))
                    .willReturn(List.of()); // 이미지 없는 케이스

            // when
            List<PostCardResponse> result = postsService.getRecentPosts(clubId, viewerId, pageable);

            // then
            assertThat(result).hasSize(1);

            then(clubAuthorizationService).should().validateAndGetClubForReadPosts(clubId, viewerId);
            then(postsRepository).should().findPostCards(clubId, pageable);
            then(postImagesRepository).should().findByPostIdIn(List.of(1L));
        }
    }

    @Nested
    class GetPostDetail { /* 상세 조회 */

        @Test
        @DisplayName("[GUEST] 공개 모임 게시글 상세 조회 성공")
        void get_post_public() {
            // given
            Long clubId = 1L;
            Long postId = 1L;
            Long viewerId = null;

            willDoNothing().given(clubAuthorizationService)
                    .validateAndGetClubForReadPosts(clubId, viewerId);

            Clubs club = newEntity(Clubs.class);
            ReflectionTestUtils.setField(club, "clubId", clubId);

            Users writer = newEntity(Users.class);
            ReflectionTestUtils.setField(writer, "userId", 10L);

            Schedules schedule = newEntity(Schedules.class);
            ReflectionTestUtils.setField(schedule, "scheduleId", 100L);

            Posts post = Posts.story(club, writer, schedule, "hello content");
            ReflectionTestUtils.setField(post, "postId", postId);

            given(postsRepository.findByPostIdAndClub_ClubId(postId, clubId))
                    .willReturn(Optional.of(post));

            // when
            PostDetailResponse res = postsService.getPost(clubId, postId, viewerId);

            // then
            assertThat(res).isNotNull();
            assertThat(res.postId()).isEqualTo(postId);
            assertThat(res.clubId()).isEqualTo(clubId);
            assertThat(res.writerId()).isEqualTo(10L);
            assertThat(res.scheduleId()).isEqualTo(100L);

            then(clubAuthorizationService).should(times(1))
                    .validateAndGetClubForReadPosts(clubId, viewerId);
            then(postsRepository).should(times(1))
                    .findByPostIdAndClub_ClubId(postId, clubId);
        }

        @Test
        @DisplayName("[MEMBER] 게시글 상세 조회 실패 - 없는 게시글")
        void get_no_post_member() {
            // given
            Long clubId = 1L;
            Long postId = 999L;
            Long viewerId = 100L;

            willDoNothing().given(clubAuthorizationService)
                    .validateAndGetClubForReadPosts(clubId, viewerId);

            given(postsRepository.findByPostIdAndClub_ClubId(postId, clubId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postsService.getPost(clubId, postId, viewerId))
                    .isInstanceOf(PostsException.PostNotFound.class);
        }

        @Test
        @DisplayName("[GUEST] 공개 모임 게시글 상세 조회 실패 - 비공개 게시글")
        void get_post_private_guest() {
            // given
            Long clubId = 1L;
            Long postId = 1L;
            Long viewerId = null; // 게스트 정책이면 null

            willThrow(new ClubAuthException.LoginRequired()) // 프로젝트 예외로 교체
                    .given(clubAuthorizationService)
                    .validateAndGetClubForReadPosts(clubId, viewerId);

            // when & then
            assertThatThrownBy(() -> postsService.getPost(clubId, postId, viewerId))
                    .isInstanceOf(ClubAuthException.class);

            then(postsRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("[MEMBER] 비공개 모임 게시글 상세 조회 성공")
        void get_post_private_member() {
            // given
            Long clubId = 1L;
            Long postId = 10L;
            Long viewerId = 100L; // 멤버

            willDoNothing().given(clubAuthorizationService)
                    .validateAndGetClubForReadPosts(clubId, viewerId);

            Clubs club = newEntity(Clubs.class);
            ReflectionTestUtils.setField(club, "clubId", clubId);

            Users writer = newEntity(Users.class);
            ReflectionTestUtils.setField(writer, "userId", 200L);

            Posts post = Posts.story(club, writer, null, "content");
            ReflectionTestUtils.setField(post, "postId", postId);

            given(postsRepository.findByPostIdAndClub_ClubId(postId, clubId))
                    .willReturn(Optional.of(post));

            // when
            PostDetailResponse res = postsService.getPost(clubId, postId, viewerId);

            // then
            assertThat(res).isNotNull();
            assertThat(res.postId()).isEqualTo(postId);

            then(clubAuthorizationService).should(times(1))
                    .validateAndGetClubForReadPosts(clubId, viewerId);
            then(postsRepository).should(times(1))
                    .findByPostIdAndClub_ClubId(postId, clubId);
        }

//        @Nested
//        @DisplayName("게시글 내 댓글 전체 조회")
//        class GetPostComments {
//
//            @Mock private ClubsAuthorizationService clubAuthorizationService;
//            @Mock private PostCommentsRepository postCommentsRepository;
//
//            @InjectMocks
//            private PostCommentsService postCommentsService;
//
//            @Test
//            @DisplayName("[GUEST] 공개 모임 댓글 조회 성공 - 페이징 메타 포함")
//            void get_comments_public_guest_success() {
//                // given
//                Long clubId = 1L;
//                Long postId = 10L;
//                Long viewerId = null;
//
//                Pageable pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "createdAt"));
//
//                willDoNothing().given(clubAuthorizationService)
//                        .validateAndGetClubForReadPosts(clubId, viewerId);
//
//                Users w1 = newEntity(Users.class);
//                ReflectionTestUtils.setField(w1, "userId", 100L);
//
//                PostComments c1 = newEntity(PostComments.class);
//                ReflectionTestUtils.setField(c1, "commentId", 1L);
//                ReflectionTestUtils.setField(c1, "writer", w1);
//                ReflectionTestUtils.setField(c1, "content", "a");
//                ReflectionTestUtils.setField(c1, "createdAt", LocalDateTime.now());
//
//                Users w2 = newEntity(Users.class);
//                ReflectionTestUtils.setField(w2, "userId", 200L);
//
//                PostComments c2 = newEntity(PostComments.class);
//                ReflectionTestUtils.setField(c2, "commentId", 2L);
//                ReflectionTestUtils.setField(c2, "writer", w2);
//                ReflectionTestUtils.setField(c2, "content", "b");
//                ReflectionTestUtils.setField(c2, "createdAt", LocalDateTime.now());
//
//                // total=5면 hasNext=true (page=0, size=2)
//                Page<PostComments> page = new PageImpl<>(List.of(c1, c2), pageable, 5);
//
//                given(postCommentsRepository.findAllByPost_PostIdAndPost_Club_ClubIdAndDeletedAtIsNull(
//                        postId, clubId, pageable
//                )).willReturn(page);
//
//                // when
//                PostCommentsResponse res = postCommentsService.getPostComments(viewerId, clubId, postId, pageable);
//
//                // then
//                assertThat(res.comments()).hasSize(2);
//                assertThat(res.page()).isEqualTo(0);
//                assertThat(res.size()).isEqualTo(2);
//                assertThat(res.totalElements()).isEqualTo(5);
//                assertThat(res.totalPages()).isEqualTo(3);
//                assertThat(res.hasNext()).isTrue();
//
//                assertThat(res.comments().get(0).commentId()).isEqualTo(1L);
//                assertThat(res.comments().get(0).writerId()).isEqualTo(100L);
//                assertThat(res.comments().get(0).content()).isEqualTo("a");
//
//                then(clubAuthorizationService).should(times(1))
//                        .validateAndGetClubForReadPosts(clubId, viewerId);
//                then(postCommentsRepository).should(times(1))
//                        .findAllByPost_PostIdAndPost_Club_ClubIdAndDeletedAtIsNull(postId, clubId, pageable);
//            }
//
//            @Test
//            @DisplayName("[MEMBER] 댓글 조회 성공 - 결과 없음")
//            void get_comments_empty() {
//                // given
//                Long clubId = 1L;
//                Long postId = 10L;
//                Long viewerId = 100L;
//
//                Pageable pageable = PageRequest.of(0, 20);
//
//                willDoNothing().given(clubAuthorizationService)
//                        .validateAndGetClubForReadPosts(clubId, viewerId);
//
//                Page<PostComments> empty = Page.empty(pageable);
//
//                given(postCommentsRepository.findAllByPost_PostIdAndPost_Club_ClubIdAndDeletedAtIsNull(
//                        postId, clubId, pageable
//                )).willReturn(empty);
//
//                // when
//                PostCommentsResponse res = postCommentsService.getPostComments(viewerId, clubId, postId, pageable);
//
//                // then
//                assertThat(res.comments()).isEmpty();
//                assertThat(res.hasNext()).isFalse();
//                assertThat(res.totalElements()).isEqualTo(0);
//            }
//
//            @Test
//            @DisplayName("[GUEST] 비공개 모임 댓글 조회 실패 - 권한 없음")
//            void get_comments_private_guest_fail() {
//                // given
//                Long clubId = 1L;
//                Long postId = 10L;
//                Long viewerId = null;
//
//                Pageable pageable = PageRequest.of(0, 20);
//
//                willThrow(new ClubAuthException.LoginRequired()) // 프로젝트 예외로 교체
//                        .given(clubAuthorizationService)
//                        .validateAndGetClubForReadPosts(clubId, viewerId);
//
//                // when & then
//                assertThatThrownBy(() -> postCommentsService.getPostComments(viewerId, clubId, postId, pageable))
//                        .isInstanceOf(ClubAuthException.class);
//
//                then(postCommentsRepository).shouldHaveNoInteractions();
//            }
//
//            private static <T> T newEntity(Class<T> type) {
//                try {
//                    var ctor = type.getDeclaredConstructor();
//                    ctor.setAccessible(true);
//                    return ctor.newInstance();
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        }

        @Nested
        @DisplayName("게시글 내 좋아요 수 조회")
        class getLikes {
        }
    }

//    @Nested
//    class CreatePost { /* 게시글 생성 */
//
//        @Test
//        @DisplayName("[MEMBER] 모임 게시글 생성 성공 - 이미지/태그 없음")
//        void create_post_member_success_without_images_tags() {
//            Long clubId = 1L;
//            Long writerId = 1L;
//
//            StoryCreateRequest request = new StoryCreateRequest(
//                    null,
//                    "모임 게시글 생성",
//                    null,
//                    "  남한산성  ",
//                    null
//            );
//
//            Clubs clubRef = club(clubId);
//            Users writerRef = user(writerId);
//
//            given(clubsRepository.getReferenceById(clubId)).willReturn(clubRef);
//            given(usersRepository.getReferenceById(writerId)).willReturn(writerRef);
//
//            Posts savedPost = Posts.story(clubRef, writerRef, null, request.content());
//            ReflectionTestUtils.setField(savedPost, "postId", 10L);
//
//            given(postsRepository.save(any(Posts.class))).willReturn(savedPost);
//
//            PostIdResponse res = postsService.createStory(clubId, writerId, request);
//
//            assertThat(res.postId()).isEqualTo(10L);
//
//            then(postsRepository).should(times(1)).save(any(Posts.class));
//            then(postImagesRepository).shouldHaveNoInteractions();
//            then(postMemberTagsRepository).shouldHaveNoInteractions();
//        }
//
//        @Test
//        @DisplayName("[MEMBER] 모임 게시글 생성 성공 - 이미지/태그 저장 포함")
//        void create_post_member_success_with_images_tags() {
//            Long clubId = 1L;
//            Long writerId = 1L;
//
//            StoryCreateRequest request = new StoryCreateRequest(
//                    1L,
//                    "모임 게시글 생성",
//                    List.of("https://example.com/1.png", "https://example.com/2.png"),
//                    "강남역",
//                    List.of(2L, 3L)
//            );
//
//            Clubs clubRef = club(clubId);
//            Users writerRef = user(writerId);
//            Schedules scheduleRef = schedule(1L);
//
//            given(clubsRepository.getReferenceById(clubId)).willReturn(clubRef);
//            given(usersRepository.getReferenceById(writerId)).willReturn(writerRef);
//            given(schedulesRepository.getReferenceById(1L)).willReturn(scheduleRef);
//
//            Posts savedPost = Posts.story(clubRef, writerRef, scheduleRef, request.content());
//            ReflectionTestUtils.setField(savedPost, "postId", 1L);
//
//            given(postsRepository.save(any(Posts.class))).willReturn(savedPost);
//
//            PostIdResponse res = postsService.createStory(clubId, writerId, request);
//
//            assertThat(res.postId()).isEqualTo(1L);
//
//            then(postImagesRepository).should(times(1)).deleteByPost_PostId(1L);
//            then(postImagesRepository).should(times(1)).saveAll(anyList());
//
//            then(postMemberTagsRepository).should(times(1)).deleteByPostId(1L);
//            then(postMemberTagsRepository).should(times(1)).saveAll(anyList());
//        }
//
//        @Test
//        @DisplayName("[GUEST] 모임 게시글 생성 실패 - 게시글 생성 권한 없음")
//        void create_post_guest() {
//            // given
//            Long clubId = 1L;
//            Long writerId = 999L; // 게스트/비회원 가정
//
//            StoryCreateRequest request = new StoryCreateRequest(
//                    null,
//                    "content",
//                    null,
//                    null,
//                    null
//            );
//
//            willThrow(new ClubAuthException.LoginRequired()) // 프로젝트 예외로 교체
//                    .given(clubAuthorizationService)
//                    .assertActiveMember(clubId, writerId);
//
//            // when & then
//            assertThatThrownBy(() -> postsService.createStory(clubId, writerId, request))
//                    .isInstanceOf(ClubAuthException.class);
//
//            then(postsRepository).shouldHaveNoInteractions();
//            then(postImagesRepository).shouldHaveNoInteractions();
//            then(postMemberTagsRepository).shouldHaveNoInteractions();
//        }
//
//        @Nested
//        class CreateComment {
//
//            @Mock private ClubsAuthorizationService clubAuthorizationService;
//            @Mock private PostsRepository postsRepository;
//            @Mock private PostCommentsRepository postCommentsRepository;
//
//            @InjectMocks
//            private PostCommentsService postCommentsService;
//
//            @Test
//            @DisplayName("[MEMBER] 댓글 생성 성공 - id 반환")
//            void create_comment_member_success() {
//                // given
//                Long clubId = 1L;
//                Long postId = 10L;
//                Long writerId = 100L;
//
//                PostCommentRequest req = new PostCommentRequest("hi");
//
//                willDoNothing().given(clubAuthorizationService)
//                        .assertActiveMember(clubId, writerId);
//
//                // posts 존재 확인을 한다면(서비스 구현에 따라)
//                Posts postRef = newEntity(Posts.class);
//                ReflectionTestUtils.setField(postRef, "postId", postId);
//                given(postsRepository.getReferenceById(postId)).willReturn(postRef);
//
//                PostComments saved = newEntity(PostComments.class);
//                ReflectionTestUtils.setField(saved, "commentId", 55L);
//                given(postCommentsRepository.save(any(PostComments.class))).willReturn(saved);
//
//                // when
//                PostCommentsIdResponse res = postCommentsService.createComment(writerId, clubId, postId, req);
//
//                // then
//                assertThat(res.commentId()).isEqualTo(55L);
//                then(postCommentsRepository).should(times(1)).save(any(PostComments.class));
//            }
//
//            @Test
//            @DisplayName("[GUEST] 댓글 생성 실패 - 권한 없음")
//            void create_comment_guest_fail() {
//                // given
//                Long clubId = 1L;
//                Long postId = 10L;
//                Long writerId = 999L;
//
//                PostCommentRequest req = new PostCommentRequest("hi");
//
//                willThrow(new ClubAuthException.LoginRequired()) // 프로젝트 예외로 교체
//                        .given(clubAuthorizationService)
//                        .assertActiveMember(clubId, writerId);
//
//                // when & then
//                assertThatThrownBy(() -> postCommentsService.createComment(writerId, clubId, postId, req))
//                        .isInstanceOf(ClubAuthException.class);
//
//                then(postCommentsRepository).shouldHaveNoInteractions();
//            }
//        }
//
//        @Nested
//        class CreateLike {
//        }
//    }

//    @Nested
//    class UpdatePost { /* 수정 */
//
//        @Test
//        @DisplayName("[MEMBER] 모임 게시글 수정 성공")
//        void update_post_member() {
//            // given
//            Long clubId = 1L;
//            Long postId = 10L;
//            Long writerId = 100L; // actor == writer
//
//            Clubs club = club(clubId);
//            Users writer = user(writerId);
//
//            Posts post = Posts.story(club, writer, null, "old content");
//            ReflectionTestUtils.setField(post, "postId", postId);
//
//            given(postsRepository.findByPostIdAndClub_ClubId(postId, clubId))
//                    .willReturn(Optional.of(post));
//
//            StoryUpdateRequest request = new StoryUpdateRequest(
//                    "new content",
//                    null,
//                    null,
//                    null
//            );
//
//            // when
//            PostIdResponse res = postsService.updatePost(clubId, postId, writerId, request);
//
//            // then
//            assertThat(res.postId()).isEqualTo(postId);
//            assertThat(post.getContent()).isEqualTo("new content");
//            assertThat(post.getPlace()).isEqualTo(null);
//
//            then(clubAuthorizationService).shouldHaveNoInteractions(); // 작성자면 매니저 체크 안 탐
//            then(postImagesRepository).shouldHaveNoInteractions();
//            then(postMemberTagsRepository).shouldHaveNoInteractions();
//        }
//
//        @Test
//        @DisplayName("[ADMIN] 모임 게시글 수정 성공")
//        void update_post_admin_ban() {
//            // given
//            Long clubId = 1L;
//            Long postId = 10L;
//            Long writerId = 100L;
//            Long adminId = 999L; // actor != writer
//
//            Clubs club = club(clubId);
//            Users writer = user(writerId);
//
//            Posts post = Posts.story(club, writer, null, "old content");
//            ReflectionTestUtils.setField(post, "postId", postId);
//
//            given(postsRepository.findByPostIdAndClub_ClubId(postId, clubId))
//                    .willReturn(Optional.of(post));
//
//            // 관리자 이상 허용
//            willDoNothing().given(clubAuthorizationService)
//                    .assertAtLeastManager(clubId, adminId);
//
//            StoryUpdateRequest request = new StoryUpdateRequest(
//                    "admin updated",
//                    List.of(),            // 빈 리스트면 전체 삭제(= delete 호출) 기대
//                    null,
//                    List.of(2L, 2L, 3L)   // 중복 포함 -> distinct 후 저장
//            );
//
//            // when
//            PostIdResponse res = postsService.updatePost(clubId, postId, adminId, request);
//
//            // then
//            assertThat(res.postId()).isEqualTo(postId);
//            assertThat(post.getContent()).isEqualTo("admin updated");
//
//            then(clubAuthorizationService).should(times(1))
//                    .assertAtLeastManager(clubId, adminId);
//
//            // imagesUrl = 빈 리스트 => delete만 호출되고 saveAll은 호출 안 됨
//            then(postImagesRepository).should(times(1)).deleteByPost_PostId(postId);
//            then(postImagesRepository).should(never()).saveAll(anyList());
//
//            // taggedMemberIds = [2,2,3] => delete + saveAll 호출
//            then(postMemberTagsRepository).should(times(1)).deleteByPostId(postId);
//            then(postMemberTagsRepository).should(times(1)).saveAll(argThat(it -> {
//                List<PostMemberTags> list = StreamSupport.stream(it.spliterator(), false).toList();
//                return list.size() == 2
//                        && list.stream().allMatch(t -> t.getPostId().equals(postId))
//                        && list.stream().map(PostMemberTags::getMemberId).collect(Collectors.toSet())
//                        .containsAll(Set.of(2L, 3L));
//            }));
//        }
//
//        @Test
//        @DisplayName("[ADMIN] 모임 게시글 수정 실패 - 게시글 수정 권한 없음")
//        void update_post_admin() {
//            // given
//            Long clubId = 1L;
//            Long postId = 10L;
//            Long writerId = 100L;
//            Long actorId = 999L; // writer 아님 + 관리자도 아님
//
//            Clubs club = club(clubId);
//            Users writer = user(writerId);
//
//            Posts post = Posts.story(club, writer, null, "old content");
//            ReflectionTestUtils.setField(post, "postId", postId);
//
//            given(postsRepository.findByPostIdAndClub_ClubId(postId, clubId))
//                    .willReturn(Optional.of(post));
//
//            willThrow(new ClubAuthException.RoleInsufficient()) // 프로젝트 예외로 교체
//                    .given(clubAuthorizationService)
//                    .assertAtLeastManager(clubId, actorId);
//
//            StoryUpdateRequest request = new StoryUpdateRequest(
//                    "new content", null, null, null
//            );
//
//            // when & then
//            assertThatThrownBy(() -> postsService.updatePost(clubId, postId, actorId, request))
//                    .isInstanceOf(ClubAuthException.class);
//
//            then(postImagesRepository).shouldHaveNoInteractions();
//            then(postMemberTagsRepository).shouldHaveNoInteractions();
//        }
//
//        @Test
//        @DisplayName("[MEMBER] 모임 게시글 수정 실패 - 게시글 수정 권한 없음, 작성자 아님")
//        void update_post_member_not_writer() {
//            // given
//            Long clubId = 1L;
//            Long postId = 10L;
//            Long writerId = 100L;
//            Long memberId = 200L; // writer 아님
//
//            Clubs club = club(clubId);
//            Users writer = user(writerId);
//
//            Posts post = Posts.story(club, writer, null, "old content");
//            ReflectionTestUtils.setField(post, "postId", postId);
//
//            given(postsRepository.findByPostIdAndClub_ClubId(postId, clubId))
//                    .willReturn(Optional.of(post));
//
//            willThrow(new ClubAuthException.LoginRequired()) // 프로젝트 예외로 교체
//                    .given(clubAuthorizationService)
//                    .assertAtLeastManager(clubId, memberId);
//
//            List <String> imagesUrl = List.of("example/1", "example/2");
//            StoryUpdateRequest request = new StoryUpdateRequest(
//                    "new content", imagesUrl , null, null
//            );
//
//            // when & then
//            assertThatThrownBy(() -> postsService.updatePost(clubId, postId, memberId, request))
//                    .isInstanceOf(ClubAuthException.class);
//
//            then(postImagesRepository).shouldHaveNoInteractions();
//            then(postMemberTagsRepository).shouldHaveNoInteractions();
//        }
//
//        // ===== 테스트 헬퍼 (테스트 클래스에 이미 있으면 재사용) =====
//
//        private Clubs club(Long id) {
//            Clubs c = newEntity(Clubs.class);
//            ReflectionTestUtils.setField(c, "clubId", id);
//            return c;
//        }
//
//        private Users user(Long id) {
//            Users u = newEntity(Users.class);
//            ReflectionTestUtils.setField(u, "userId", id);
//            return u;
//        }
//
//        private static <T> T newEntity(Class<T> type) {
//            try {
//                var ctor = type.getDeclaredConstructor();
//                ctor.setAccessible(true);
//                return ctor.newInstance();
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        }
//
//
//        @Nested
//        class UpdateComment {
//
//            @Test
//            @DisplayName("[MEMBER] 댓글 수정 성공 - 작성자")
//            void update_comment_writer_success() {
//                // given
//                Long clubId = 1L;
//                Long postId = 10L;
//                Long commentId = 100L;
//                Long writerId = 7L;
//
//                PostCommentRequest req = new PostCommentRequest("updated");
//
//                Users writer = newEntity(Users.class);
//                ReflectionTestUtils.setField(writer, "userId", writerId);
//
//                Posts post = newEntity(Posts.class);
//                PostComments comment = newEntity(PostComments.class);
//                ReflectionTestUtils.setField(comment, "commentId", commentId);
//                ReflectionTestUtils.setField(comment, "posts", post);
//                ReflectionTestUtils.setField(comment, "writer", writer);
//                ReflectionTestUtils.setField(comment, "content", "old");
//                ReflectionTestUtils.setField(comment, "deletedAt", null);
//
//                given(postCommentsRepository.findByIdAndPost_PostIdAndPost_Club_ClubId(commentId, postId, clubId))
//                        .willReturn(Optional.of(comment));
//
//                // when
//                PostCommentsIdResponse res = postCommentsService.updateComment(writerId, clubId, postId, commentId, req);
//
//                // then
//                assertThat(res.commentId()).isEqualTo(commentId);
//                assertThat(comment.getContent()).isEqualTo("updated");
//                then(clubAuthorizationService).shouldHaveNoInteractions(); // 작성자면 매니저 체크 안 함
//            }
//
//            @Test
//            @DisplayName("[ADMIN] 댓글 수정 성공 - 작성자 아님, 운영진 이상")
//            void update_comment_manager_success() {
//                // given
//                Long clubId = 1L;
//                Long postId = 10L;
//                Long commentId = 100L;
//                Long writerId = 7L;
//                Long managerId = 8L;
//
//                PostCommentRequest req = new PostCommentRequest("updated");
//
//                Users writer = newEntity(Users.class);
//                ReflectionTestUtils.setField(writer, "userId", writerId);
//
//                PostComments comment = newEntity(PostComments.class);
//                ReflectionTestUtils.setField(comment, "commentId", commentId);
//                ReflectionTestUtils.setField(comment, "writer", writer);
//                ReflectionTestUtils.setField(comment, "content", "old");
//                ReflectionTestUtils.setField(comment, "deletedAt", null);
//
//                given(postCommentsRepository.findByIdAndPost_PostIdAndPost_Club_ClubId(commentId, postId, clubId))
//                        .willReturn(Optional.of(comment));
//
//                willDoNothing().given(clubAuthorizationService)
//                        .assertAtLeastManager(clubId, managerId);
//
//                // when
//                PostCommentsIdResponse res = postCommentsService.updateComment(managerId, clubId, postId, commentId, req);
//
//                // then
//                assertThat(res.commentId()).isEqualTo(commentId);
//                assertThat(comment.getContent()).isEqualTo("updated");
//                then(clubAuthorizationService).should(times(1)).assertAtLeastManager(clubId, managerId);
//            }
//
//            @Test
//            @DisplayName("[MEMBER] 댓글 수정 실패 - 작성자 아님, 운영진 권한도 없음")
//            void update_comment_forbidden() {
//                // given
//                Long clubId = 1L;
//                Long postId = 10L;
//                Long commentId = 100L;
//                Long writerId = 7L;
//                Long actorId = 9L;
//
//                PostCommentRequest req = new PostCommentRequest("updated");
//
//                Users writer = newEntity(Users.class);
//                ReflectionTestUtils.setField(writer, "userId", writerId);
//
//                PostComments comment = newEntity(PostComments.class);
//                ReflectionTestUtils.setField(comment, "commentId", commentId);
//                ReflectionTestUtils.setField(comment, "writer", writer);
//                ReflectionTestUtils.setField(comment, "content", "old");
//                ReflectionTestUtils.setField(comment, "deletedAt", null);
//
//                given(postCommentsRepository.findByIdAndPost_PostIdAndPost_Club_ClubId(commentId, postId, clubId))
//                        .willReturn(Optional.of(comment));
//
//                willThrow(new ClubAuthException.LoginRequired()) // 프로젝트 예외로 교체
//                        .given(clubAuthorizationService)
//                        .assertAtLeastManager(clubId, actorId);
//
//                // when & then
//                assertThatThrownBy(() -> postCommentsService.updateComment(actorId, clubId, postId, commentId, req))
//                        .isInstanceOf(ClubAuthException.class);
//
//                assertThat(comment.getContent()).isEqualTo("old");
//            }
//
//            @Test
//            @DisplayName("댓글 수정 실패 - 이미 삭제된 댓글")
//            void update_deleted_comment_fail() {
//                // given
//                Long clubId = 1L;
//                Long postId = 10L;
//                Long commentId = 100L;
//                Long actorId = 7L;
//
//                PostCommentRequest req = new PostCommentRequest("updated");
//
//                Users writer = newEntity(Users.class);
//                ReflectionTestUtils.setField(writer, "userId", actorId);
//
//                PostComments comment = newEntity(PostComments.class);
//                ReflectionTestUtils.setField(comment, "commentId", commentId);
//                ReflectionTestUtils.setField(comment, "writer", writer);
//                ReflectionTestUtils.setField(comment, "content", "old");
//                ReflectionTestUtils.setField(comment, "deletedAt", LocalDateTime.now()); // 삭제됨
//
//                given(postCommentsRepository.findByIdAndPost_PostIdAndPost_Club_ClubId(commentId, postId, clubId))
//                        .willReturn(Optional.of(comment));
//
//                // when & then
//                assertThatThrownBy(() -> postCommentsService.updateComment(actorId, clubId, postId, commentId, req))
//                        .isInstanceOf(PostsException.Deleted.class); // 프로젝트 예외로 교체
//            }
//
//            private static <T> T newEntity(Class<T> type) {
//                try {
//                    var ctor = type.getDeclaredConstructor();
//                    ctor.setAccessible(true);
//                    return ctor.newInstance();
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        }
//
//    }


    @Nested
    class DeletePost { /* 삭제 */

        @Test
        @DisplayName("[MEMBER] 모임 게시글 삭제 성공")
        void delete_post_member() {
            // given
            Long clubId = 1L;
            Long postId = 10L;
            Long writerId = 100L; // 작성자

            Clubs club = club(clubId);
            Users writer = user(writerId);

            Posts post = Posts.story(club, writer, null, "content");
            ReflectionTestUtils.setField(post, "postId", postId);

            given(postsRepository.findByPostIdAndClub_ClubId(postId, clubId))
                    .willReturn(Optional.of(post));

            // when
            postsService.deletePost(clubId, postId, writerId);

            // then
            assertThat(post.getDeletedAt()).isNotNull();

            // 작성자면 매니저 체크 안 탐
            then(clubAuthorizationService).shouldHaveNoInteractions();
            then(postsRepository).should(times(1)).findByPostIdAndClub_ClubId(postId, clubId);
        }

        @Test
        @DisplayName("[MEMBER] 모임 게시글 삭제 실패 - 게시글 삭제 권한 없음, 작성자 아님")
        void delete_post_guest() {
            // given
            Long clubId = 1L;
            Long postId = 10L;
            Long writerId = 100L;
            Long memberId = 200L; // 작성자 아님

            Clubs club = club(clubId);
            Users writer = user(writerId);

            Posts post = Posts.story(club, writer, null, "content");
            ReflectionTestUtils.setField(post, "postId", postId);

            given(postsRepository.findByPostIdAndClub_ClubId(postId, clubId))
                    .willReturn(Optional.of(post));

            willThrow(new ClubAuthException.LoginRequired()) // 프로젝트 예외로 교체
                    .given(clubAuthorizationService)
                    .assertAtLeastManager(clubId, memberId);

            // when & then
            assertThatThrownBy(() -> postsService.deletePost(clubId, postId, memberId))
                    .isInstanceOf(ClubAuthException.class);

            // delete 안 됨
            assertThat(post.getDeletedAt()).isNull();
            then(clubAuthorizationService).should(times(1)).assertAtLeastManager(clubId, memberId);
        }

        // ===== 테스트 헬퍼 (이미 있으면 재사용) =====
        private Clubs club(Long id) {
            Clubs c = newEntity(Clubs.class);
            ReflectionTestUtils.setField(c, "clubId", id);
            return c;
        }

        private Users user(Long id) {
            Users u = newEntity(Users.class);
            ReflectionTestUtils.setField(u, "userId", id);
            return u;
        }

        private static <T> T newEntity(Class<T> type) {
            try {
                var ctor = type.getDeclaredConstructor();
                ctor.setAccessible(true);
                return ctor.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Nested
        class DeleteComment {

            @Test
            @DisplayName("[MEMBER] 댓글 삭제 성공 - 작성자")
            void delete_comment_writer_success() {
                // given
                Long clubId = 1L;
                Long postId = 10L;
                Long commentId = 100L;
                Long writerId = 7L;

                Users writer = newEntity(Users.class);
                ReflectionTestUtils.setField(writer, "userId", writerId);

                Comments comment = newEntity(Comments.class);
                ReflectionTestUtils.setField(comment, "commentId", commentId);
                ReflectionTestUtils.setField(comment, "writer", writer);
                ReflectionTestUtils.setField(comment, "deletedAt", null);

                given(postCommentsRepository.findByCommentIdAndPost_PostIdAndPost_Club_ClubIdAndDeletedAtIsNull(commentId, postId, clubId))
                        .willReturn(Optional.of(comment));

                // when
                PostCommentsIdResponse res = postCommentsService.deleteComment(writerId, clubId, postId, commentId);

                // then
                assertThat(res.commentId()).isEqualTo(commentId);
                assertThat(comment.getDeletedAt()).isNotNull(); // soft delete 확인
                then(clubAuthorizationService).shouldHaveNoInteractions(); // 작성자면 매니저 체크 안 함
            }

//            @Test
//            @DisplayName("[ADMIN] 댓글 삭제 성공 - 작성자 아님, 운영진 이상")
//            void delete_comment_manager_success() {
//                // given
//                Long clubId = 1L;
//                Long postId = 10L;
//                Long commentId = 100L;
//                Long writerId = 7L;
//                Long managerId = 8L;
//
//                Users writer = newEntity(Users.class);
//                ReflectionTestUtils.setField(writer, "userId", writerId);
//
//                PostComments comment = newEntity(PostComments.class);
//                ReflectionTestUtils.setField(comment, "commentId", commentId);
//                ReflectionTestUtils.setField(comment, "writer", writer);
//                ReflectionTestUtils.setField(comment, "deletedAt", null);
//
//                given(postCommentsRepository.findByCommentIdAndPost_PostIdAndPost_Club_ClubIdAndDeletedAtIsNull(commentId, postId, clubId))
//                        .willReturn(Optional.of(comment));
//
//                willDoNothing().given(clubAuthorizationService)
//                        .assertAtLeastManager(clubId, managerId);
//
//                // when
//                PostCommentsIdResponse res = postCommentsService.deleteComment(managerId, clubId, postId, commentId);
//
//                // then
//                assertThat(res.commentId()).isEqualTo(commentId);
//                assertThat(comment.getDeletedAt()).isNotNull();
//                then(clubAuthorizationService).should(times(1)).assertAtLeastManager(clubId, managerId);
//            }

//            @Test
//            @DisplayName("[MEMBER] 댓글 삭제 실패 - 작성자 아님, 운영진 권한도 없음")
//            void delete_comment_forbidden() {
//                // given
//                Long clubId = 1L;
//                Long postId = 10L;
//                Long commentId = 100L;
//                Long writerId = 7L;
//                Long actorId = 9L;
//
//                Users writer = newEntity(Users.class);
//                ReflectionTestUtils.setField(writer, "userId", writerId);
//
//                PostComments comment = newEntity(PostComments.class);
//                ReflectionTestUtils.setField(comment, "commentId", commentId);
//                ReflectionTestUtils.setField(comment, "writer", writer);
//                ReflectionTestUtils.setField(comment, "deletedAt", null);
//
//                given(postCommentsRepository.findByIdAndPost_PostIdAndPost_Club_ClubId(commentId, postId, clubId))
//                        .willReturn(Optional.of(comment));
//
//                willThrow(new ClubAuthException.LoginRequired()) // 프로젝트 예외로 교체
//                        .given(clubAuthorizationService)
//                        .assertAtLeastManager(clubId, actorId);
//
//                // when & then
//                assertThatThrownBy(() -> postCommentsService.deleteComment(actorId, clubId, postId, commentId))
//                        .isInstanceOf(ClubAuthException.class);
//
//                assertThat(comment.getDeletedAt()).isNull(); // 삭제 안 됨
//            }

            @Test
            @DisplayName("댓글 삭제 멱등 - 이미 삭제된 댓글은 다시 삭제해도 성공")
            void delete_comment_idempotent_when_already_deleted() {
                // given
                Long clubId = 1L;
                Long postId = 10L;
                Long commentId = 100L;
                Long actorId = 7L;

                Users writer = newEntity(Users.class);
                ReflectionTestUtils.setField(writer, "userId", 999L); // actor가 작성자 아니어도 상관 없음(이미 삭제면 return)

                Comments comment = newEntity(Comments.class);
                ReflectionTestUtils.setField(comment, "commentId", commentId);
                ReflectionTestUtils.setField(comment, "writer", writer);
                ReflectionTestUtils.setField(comment, "deletedAt", LocalDateTime.now()); // 이미 삭제됨

                given(postCommentsRepository.findByCommentIdAndPost_PostIdAndPost_Club_ClubIdAndDeletedAtIsNull(commentId, postId, clubId))
                        .willReturn(Optional.of(comment));

                // when
                PostCommentsIdResponse res = postCommentsService.deleteComment(actorId, clubId, postId, commentId);

                // then
                assertThat(res.commentId()).isEqualTo(commentId);
                then(clubAuthorizationService).shouldHaveNoInteractions(); // 멱등 return이라 권한 체크 안 타는 게 깔끔
            }

            private static <T> T newEntity(Class<T> type) {
                try {
                    var ctor = type.getDeclaredConstructor();
                    ctor.setAccessible(true);
                    return ctor.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
