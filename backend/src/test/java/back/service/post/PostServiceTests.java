package back.service.post;

import back.domain.club.Clubs;
import back.domain.club.ClubMembers;

import back.domain.post.Posts;
import back.domain.schedule.Schedules;
import back.dto.post.post.response.PostCardBase;
import back.dto.post.post.response.PostCardResponse;
import back.dto.post.post.response.PostIdResponse;
import back.dto.post.story.request.StoryCreateRequest;
import back.dto.post.story.request.StoryUpdateRequest;
import back.dto.post.story.response.PostDetailResponse;
import back.exception.PostsException;
import back.repository.club.ClubMemberRepository;
import back.repository.club.ClubRepository;
import back.repository.post.*;
import back.repository.schedule.ScheduleRepository;
import back.service.club.ClubAuthService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTests {

        @Mock private ClubAuthService clubAuthService;
        @Mock private ClubRepository clubRepository;
        @Mock private ClubMemberRepository clubMemberRepository;
        @Mock private ScheduleRepository scheduleRepository;

        @Mock private PostRepository postRepository;
        @Mock private PostImageRepository postImageRepository;
        @Mock private PostMemberTagRepository postMemberTagRepository;
        @Mock private PostLikeRepository postLikeRepository;
        @Mock
        private ApplicationEventPublisher eventPublisher;
        @InjectMocks
        private PostService postService;

        @BeforeEach
        void setUp() {
                // Optional 의존성 명시적으로 비활성화
                ReflectionTestUtils.setField(postService, "postVectorService", Optional.empty());
        }

        // =========================================================
        // Helper
        // =========================================================

        private <T> T newEntity(Class<T> type) {
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

        private ClubMembers member(Long clubId, Long userId) {
                ClubMembers m = ClubMembers.builder()
                        .clubId(clubId)
                        .userId(userId)
                        .nickname("닉네임")
                        .build();
                ReflectionTestUtils.setField(m, "memberId", userId);
                m.approve();
                return m;
        }

        private Schedules schedule(Long id) {
                Schedules s = newEntity(Schedules.class);
                ReflectionTestUtils.setField(s, "scheduleId", id);
                return s;
        }

        // =========================================================
        // 게시글 목록 조회
        // =========================================================

        @Test
        @DisplayName("[GUEST] 공개 모임 게시글 조회 성공")
        void list_posts_guest_success() {
                Long clubId = 1L;
                Pageable pageable = PageRequest.of(0, 10);

                willDoNothing().given(clubAuthService)
                        .validateAndGetClubForReadPosts(clubId, null);

                PostCardBase p1 = mock(PostCardBase.class);
                given(p1.postId()).willReturn(1L);

                given(postRepository.findPostCards(eq(clubId), eq(pageable)))
                        .willReturn(new PageImpl<>(List.of(p1), pageable, 1));

                given(postImageRepository.findByPostIdIn(List.of(1L)))
                        .willReturn(List.of());

                List<PostCardResponse> result =
                        postService.getRecentPosts(clubId, null, pageable);

                assertThat(result).hasSize(1);
                then(postRepository).should().findPostCards(clubId, pageable);
        }

        // =========================================================
        // 게시글 상세 조회
        // =========================================================

        @Test
        @DisplayName("[MEMBER] 게시글 상세 조회 성공")
        void get_post_detail_member() {
                Long clubId = 1L;
                Long postId = 10L;
                Long viewerId = 5L;

                willDoNothing().given(clubAuthService)
                        .validateAndGetClubForReadPosts(clubId, viewerId);

                Clubs club = club(clubId);
                ClubMembers writer = member(clubId, 3L);
                Schedules schedule = schedule(100L);

                Posts post = Posts.story(club, writer, schedule, "content");
                ReflectionTestUtils.setField(post, "postId", postId);

                given(postRepository.findByPostIdAndClub_ClubId(postId, clubId))
                        .willReturn(Optional.of(post));
                given(postImageRepository.findByPostIdIn(List.of(postId)))
                        .willReturn(List.of());
                given(postLikeRepository.countByPostId(postId)).willReturn(0L);
                given(postLikeRepository.existsByPostIdAndUserId(postId, viewerId))
                        .willReturn(false);

                PostDetailResponse res =
                        postService.getPost(clubId, postId, viewerId);

                assertThat(res.postId()).isEqualTo(postId);
                assertThat(res.scheduleId()).isEqualTo(100L);
        }

        // =========================================================
        // 게시글 생성
        // =========================================================

        @Test
        @DisplayName("[MEMBER] 게시글 생성 성공 - 이미지/태그 없음")
        void create_post_simple() {
                Long clubId = 1L;
                Long writerId = 1L;

                StoryCreateRequest req = new StoryCreateRequest(
                        null,
                        "content",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

                Clubs club = club(clubId);
                ClubMembers writer = member(clubId, writerId);

                willDoNothing().given(clubAuthService)
                        .assertActiveMember(clubId, writerId);
                given(clubRepository.getReferenceById(clubId)).willReturn(club);
                given(clubMemberRepository.findByClubIdAndUserId(clubId, writerId))
                        .willReturn(Optional.of(writer));
                given(clubMemberRepository.getReferenceById(writerId))
                        .willReturn(writer);

                Posts saved = Posts.story(club, writer, null, req.content());
                ReflectionTestUtils.setField(saved, "postId", 10L);

                given(postRepository.save(any())).willReturn(saved);

                PostIdResponse res = postService.createStory(clubId, writerId, req);

                assertThat(res.postId()).isEqualTo(10L);
                then(postImageRepository).shouldHaveNoInteractions();
                then(postMemberTagRepository).shouldHaveNoInteractions();
        }

        // =========================================================
        // 게시글 수정
        // =========================================================

        @Test
        @DisplayName("[WRITER] 게시글 수정 성공")
        void update_post_writer() {
                Long clubId = 1L;
                Long postId = 10L;
                Long writerId = 1L;

                Clubs club = club(clubId);
                ClubMembers writer = member(clubId, writerId);
                Posts post = Posts.story(club, writer, null, "old");
                ReflectionTestUtils.setField(post, "postId", postId);

                given(postRepository.findByPostIdAndClub_ClubId(postId, clubId))
                        .willReturn(Optional.of(post));

                StoryUpdateRequest req =
                        new StoryUpdateRequest("new", null, null, null);

                PostIdResponse res =
                        postService.updatePost(clubId, postId, writerId, req);

                assertThat(res.postId()).isEqualTo(postId);
                assertThat(post.getContent()).isEqualTo("new");
        }

        // =========================================================
        // 게시글 삭제
        // =========================================================

        @Test
        @DisplayName("[WRITER] 게시글 삭제 성공")
        void delete_post_writer() {
                Long clubId = 1L;
                Long postId = 10L;
                Long writerId = 1L;

                Clubs club = club(clubId);
                ClubMembers writer = member(clubId, writerId);
                Posts post = Posts.story(club, writer, null, "content");
                ReflectionTestUtils.setField(post, "postId", postId);

                given(postRepository.findByPostIdAndClub_ClubId(postId, clubId))
                        .willReturn(Optional.of(post));

                postService.deletePost(clubId, postId, writerId);

                assertThat(post.getDeletedAt()).isNotNull();
        }

        // =========================================================
        // 예외
        // =========================================================

        @Test
        @DisplayName("게시글 없음 → 예외")
        void post_not_found() {
                given(postRepository.findByPostIdAndClub_ClubId(1L, 1L))
                        .willReturn(Optional.empty());

                assertThatThrownBy(() ->
                        postService.getPost(1L, 1L, 1L))
                        .isInstanceOf(PostsException.PostNotFound.class);
        }
}
