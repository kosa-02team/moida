package back.listener;

import back.domain.NotificationType;
import back.domain.Notifications;
import back.domain.club.ClubMembers;
import back.dto.NotificationResponse;
import back.event.ClubJoinEvent;
import back.event.CommentCreatedEvent;
import back.event.PostCreatedEvent;
import back.event.ScheduleRegisteredEvent;
import back.event.VoteDeadlineEvent;
import back.repository.club.ClubMemberRepository;
import back.repository.notifications.NotificationsRepository;
import back.service.notifications.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private NotificationsRepository notificationsRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationEventListener notificationEventListener;

    @Test
    @DisplayName("일정 등록 시 알림이 발송되어야 한다")
    void handleScheduleRegisteredEvent() {
        // given
        Long clubId = 1L;
        Long scheduleId = 100L;
        Long authorId = 10L;
        Long otherMemberId = 11L;

        ScheduleRegisteredEvent event = new ScheduleRegisteredEvent(clubId, scheduleId, "테스트 일정", authorId);

        ClubMembers author = mock(ClubMembers.class);
        given(author.getUserId()).willReturn(authorId);

        ClubMembers otherMember = mock(ClubMembers.class);
        given(otherMember.getUserId()).willReturn(otherMemberId);

        given(clubMemberRepository.findByClubIdAndStatus(clubId, ClubMembers.Status.ACTIVE))
                .willReturn(List.of(author, otherMember));

        Notifications notification = new Notifications(otherMemberId, "content", scheduleId,
                NotificationType.SCHEDULE.name());
        given(notificationsRepository.saveAll(anyList())).willReturn(List.of(notification));

        // when
        notificationEventListener.handleScheduleRegisteredEvent(event);

        // then
        // 작성자(authorId)는 제외되고 otherMemberId에게만 전송되어야 함
        verify(notificationService).send(eq(otherMemberId), any(NotificationResponse.class));
        verify(notificationService, never()).send(eq(authorId), any(NotificationResponse.class));
    }

    @Test
    @DisplayName("댓글 작성 시 게시글 작성자에게 알림이 발송되어야 한다")
    void handleCommentCreatedEvent() {
        // given
        Long postId = 200L;
        Long commentAuthorId = 20L;
        Long postAuthorId = 10L;

        CommentCreatedEvent event = new CommentCreatedEvent(postId, "댓글 내용", commentAuthorId, postAuthorId);

        Notifications notification = new Notifications(postAuthorId, "content", postId,
                NotificationType.COMMENT.name());
        given(notificationsRepository.saveAll(anyList())).willReturn(List.of(notification));

        // when
        notificationEventListener.handleCommentCreatedEvent(event);

        // then
        verify(notificationService).send(eq(postAuthorId), any(NotificationResponse.class));
    }

    @Test
    @DisplayName("본인이 본인 글에 댓글을 달면 알림이 발송되지 않아야 한다")
    void handleCommentCreatedEvent_Self() {
        // given
        Long postId = 200L;
        Long authorId = 10L;

        CommentCreatedEvent event = new CommentCreatedEvent(postId, "댓글 내용", authorId, authorId);

        // when
        notificationEventListener.handleCommentCreatedEvent(event);

        // then
        verify(notificationsRepository, never()).saveAll(anyList());
        verify(notificationService, never()).send(anyLong(), any());
    }

    @Test
    @DisplayName("모임 가입 승인 시 가입자에게 환영 알림이 발송되어야 한다")
    void handleClubJoinEvent() {
        // given
        Long clubId = 1L;
        Long memberId = 50L;
        Long userId = 100L;
        String clubName = "테스트 모임";

        ClubJoinEvent event = new ClubJoinEvent(clubId, memberId, userId, clubName);

        Notifications notification = new Notifications(userId, "content", clubId, NotificationType.CLUB_WELCOME.name());
        given(notificationsRepository.saveAll(anyList())).willReturn(List.of(notification));

        // when
        notificationEventListener.handleClubJoinEvent(event);

        // then
        verify(notificationService).send(eq(userId), any(NotificationResponse.class));
    }

    @Test
    @DisplayName("투표 마감 임박 시 모든 멤버에게 알림이 발송되어야 한다")
    void handleVoteDeadlineEvent() {
        // given
        Long clubId = 1L;
        Long voteId = 300L;
        Long memberId1 = 10L;
        Long memberId2 = 11L;

        VoteDeadlineEvent event = new VoteDeadlineEvent(voteId, "투표 제목", clubId);

        ClubMembers member1 = mock(ClubMembers.class);
        given(member1.getUserId()).willReturn(memberId1);
        ClubMembers member2 = mock(ClubMembers.class);
        given(member2.getUserId()).willReturn(memberId2);

        given(clubMemberRepository.findByClubIdAndStatus(clubId, ClubMembers.Status.ACTIVE))
                .willReturn(List.of(member1, member2));

        Notifications noti1 = new Notifications(memberId1, "content", voteId, NotificationType.VOTE_DEADLINE.name());
        Notifications noti2 = new Notifications(memberId2, "content", voteId, NotificationType.VOTE_DEADLINE.name());

        given(notificationsRepository.saveAll(anyList())).willReturn(List.of(noti1, noti2));

        // when
        notificationEventListener.handleVoteDeadlineEvent(event);

        // then
        verify(notificationService).send(eq(memberId1), any(NotificationResponse.class));
        verify(notificationService).send(eq(memberId2), any(NotificationResponse.class));
    }
}
