package back.service.post;

import back.domain.club.ClubMembers;
import back.domain.post.Comments;
import back.domain.post.Posts;
import back.dto.post.comment.request.PostCommentRequest;
import back.dto.post.comment.response.PostCommentsIdResponse;
import back.dto.post.comment.response.PostCommentsResponse;
import back.event.CommentCreatedEvent;
import back.exception.ClubException;
import back.exception.PostsException;
import back.repository.club.ClubMemberRepository;
import back.repository.post.PostCommentRepository;
import back.repository.post.PostRepository;
import back.service.club.ClubAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PostCommentServiceTest {

    @Mock private PostCommentRepository postCommentRepository;
    @Mock private ClubMemberRepository clubMemberRepository;
    @Mock private PostRepository postRepository;
    @Mock private ClubAuthService clubAuthService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private CommentLikeService commentLikeService;

    @InjectMocks
    private PostCommentService postCommentService;

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

    private ClubMembers member(Long userId) {
        ClubMembers m = newEntity(ClubMembers.class);
        ReflectionTestUtils.setField(m, "userId", userId);
        return m;
    }

    private Posts post(Long postId, Long writerId) {
        Posts p = newEntity(Posts.class);
        ReflectionTestUtils.setField(p, "postId", postId);

        ClubMembers writer = member(writerId);
        ReflectionTestUtils.setField(p, "writer", writer);

        return p;
    }

    private Comments comment(Long commentId, Long writerId, Posts post) {
        Comments c = newEntity(Comments.class);
        ReflectionTestUtils.setField(c, "commentId", commentId);
        ReflectionTestUtils.setField(c, "writer", member(writerId));
        ReflectionTestUtils.setField(c, "post", post);
        ReflectionTestUtils.setField(c, "content", "comment");
        ReflectionTestUtils.setField(c, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(c, "deletedAt", null);
        return c;
    }

    // =========================================================
    // 댓글 생성
    // =========================================================

    @Test
    @DisplayName("[MEMBER] 댓글 생성 성공")
    void create_comment_success() {
        Long clubId = 1L;
        Long postId = 10L;
        Long writerId = 100L;

        PostCommentRequest req = new PostCommentRequest("hello");

        willDoNothing().given(clubAuthService)
                .assertActiveMember(clubId, writerId);

        Posts post = post(postId, 200L);
        given(postRepository.getReferenceById(postId)).willReturn(post);

        ClubMembers writer = member(writerId);
        given(clubMemberRepository.findByClubIdAndUserId(clubId, writerId))
                .willReturn(Optional.of(writer));

        Comments saved = comment(1L, writerId, post);
        given(postCommentRepository.save(any())).willReturn(saved);

        PostCommentsIdResponse res =
                postCommentService.createComment(writerId, clubId, postId, req);

        assertThat(res.commentId()).isEqualTo(1L);
        then(eventPublisher)
                .should()
                .publishEvent(any(CommentCreatedEvent.class));    }

    @Test
    @DisplayName("[GUEST] 댓글 생성 실패 - 권한 없음")
    void create_comment_guest_fail() {
        willThrow(new ClubException.AuthLoginRequired())
                .given(clubAuthService)
                .assertActiveMember(anyLong(), anyLong());

        assertThatThrownBy(() ->
                postCommentService.createComment(1L, 1L, 1L, new PostCommentRequest("x")))
                .isInstanceOf(ClubException.class);

        then(postCommentRepository).shouldHaveNoInteractions();
    }

    // =========================================================
    // 댓글 조회
    // =========================================================

    @Test
    @DisplayName("[GUEST] 댓글 목록 조회 성공")
    void get_comments_guest_success() {
        Long clubId = 1L;
        Long postId = 10L;

        Pageable pageable = PageRequest.of(0, 2, Sort.by("createdAt"));

        willDoNothing().given(clubAuthService)
                .validateAndGetClubForReadPosts(clubId, null);

        Posts post = post(postId, 100L);
        Comments c1 = comment(1L, 101L, post);
        Comments c2 = comment(2L, 102L, post);

        Page<Comments> page =
                new PageImpl<>(List.of(c1, c2), pageable, 2);

        given(postCommentRepository
                .findAllByPost_PostIdAndPost_Club_ClubIdAndDeletedAtIsNull(
                        postId, clubId, pageable))
                .willReturn(page);

        given(commentLikeService.getLikeCount(1L)).willReturn(0L);
        given(commentLikeService.getLikeCount(2L)).willReturn(0L);
        given(commentLikeService.isLiked(anyLong(), any())).willReturn(false);

        PostCommentsResponse res =
                postCommentService.getPostComments(null, clubId, postId, pageable);

        assertThat(res.comments()).hasSize(2);
        assertThat(res.hasNext()).isFalse();
    }

    // =========================================================
    // 댓글 수정
    // =========================================================

    @Test
    @DisplayName("[WRITER] 댓글 수정 성공")
    void update_comment_writer_success() {
        Long clubId = 1L;
        Long postId = 10L;
        Long commentId = 100L;
        Long writerId = 7L;

        Posts post = post(postId, 99L);
        Comments comment = comment(commentId, writerId, post);

        given(postCommentRepository
                .findByCommentIdAndPost_PostIdAndPost_Club_ClubIdAndDeletedAtIsNull(
                        commentId, postId, clubId))
                .willReturn(Optional.of(comment));

        PostCommentsIdResponse res =
                postCommentService.updateComment(
                        writerId, clubId, postId, commentId,
                        new PostCommentRequest("updated"));

        assertThat(res.commentId()).isEqualTo(commentId);
        assertThat(comment.getContent()).isEqualTo("updated");
    }

    @Test
    @DisplayName("댓글 수정 실패 - 이미 삭제됨")
    void update_deleted_comment_fail() {
        Comments comment = newEntity(Comments.class);
        ReflectionTestUtils.setField(comment, "deletedAt", LocalDateTime.now());

        given(postCommentRepository
                .findByCommentIdAndPost_PostIdAndPost_Club_ClubIdAndDeletedAtIsNull(
                        any(), any(), any()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                postCommentService.updateComment(
                        1L, 1L, 1L, 1L, new PostCommentRequest("x")))
                .isInstanceOf(PostsException.PostCommentNotFound.class);
    }

    // =========================================================
    // 댓글 삭제
    // =========================================================

    @Test
    @DisplayName("[WRITER] 댓글 삭제 성공")
    void delete_comment_writer_success() {
        Long clubId = 1L;
        Long postId = 10L;
        Long commentId = 100L;
        Long writerId = 7L;

        Posts post = post(postId, 99L);
        Comments comment = comment(commentId, writerId, post);

        given(postCommentRepository
                .findByCommentIdAndPost_PostIdAndPost_Club_ClubIdAndDeletedAtIsNull(
                        commentId, postId, clubId))
                .willReturn(Optional.of(comment));

        PostCommentsIdResponse res =
                postCommentService.deleteComment(writerId, clubId, postId, commentId);

        assertThat(res.commentId()).isEqualTo(commentId);
        assertThat(comment.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("댓글 삭제 멱등 - 이미 삭제된 댓글")
    void delete_comment_idempotent() {
        Comments comment = newEntity(Comments.class);
        ReflectionTestUtils.setField(comment, "commentId", 1L);
        ReflectionTestUtils.setField(comment, "deletedAt", LocalDateTime.now());

        given(postCommentRepository
                .findByCommentIdAndPost_PostIdAndPost_Club_ClubIdAndDeletedAtIsNull(
                        any(), any(), any()))
                .willReturn(Optional.of(comment));

        PostCommentsIdResponse res =
                postCommentService.deleteComment(1L, 1L, 1L, 1L);

        assertThat(res.commentId()).isEqualTo(1L);
        then(clubAuthService).shouldHaveNoInteractions();
    }
}
