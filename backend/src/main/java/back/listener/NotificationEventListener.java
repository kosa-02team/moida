package back.listener;

import back.domain.NotificationType;
import back.domain.Notifications;
import back.domain.club.ClubMembers;
import back.dto.NotificationResponse;
import back.event.CommentCreatedEvent;
import back.event.PostCreatedEvent;
import back.event.ScheduleRegisteredEvent;
import back.repository.club.ClubMemberRepository;
import back.repository.notifications.NotificationsRepository;
import back.service.notifications.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final ClubMemberRepository clubMemberRepository;
    private final NotificationsRepository notificationsRepository;
    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener
    public void handleScheduleRegisteredEvent(ScheduleRegisteredEvent event) {
        List<ClubMembers> members = clubMemberRepository.findByClubIdAndStatus(event.getClubId(),
                ClubMembers.Status.ACTIVE);

        List<Notifications> notifications = members.stream()
                .filter(member -> !member.getUserId().equals(event.getAuthorId())) // 본인 제외
                .map(member -> new Notifications(
                        member.getUserId(),
                        "새로운 일정 '" + event.getScheduleName() + "'이 등록 되었습니다.",
                        event.getScheduleId(),
                        NotificationType.SCHEDULE.name()))
                .collect(Collectors.toList());

        saveAndSend(notifications);
    }

    @Async
    @TransactionalEventListener
    public void handlePostCreatedEvent(PostCreatedEvent event) {
        List<ClubMembers> members = clubMemberRepository.findByClubIdAndStatus(event.getClubId(),
                ClubMembers.Status.ACTIVE);

        List<Notifications> notifications = members.stream()
                .filter(member -> !member.getUserId().equals(event.getAuthorId()))
                .map(member -> new Notifications(
                        member.getUserId(),
                        "새로운 게시글 '" + event.getPostTitle() + "'이 등록 되었습니다.",
                        event.getPostId(),
                        NotificationType.POST.name()))
                .collect(Collectors.toList());

        saveAndSend(notifications);
    }

    @Async
    @TransactionalEventListener
    public void handleCommentCreatedEvent(CommentCreatedEvent event) {
        if (event.getCommentAuthorId().equals(event.getPostAuthorId())) {
            return; // 본인 글에 댓글 단 경우 알림 생략
        }

        Notifications notification = new Notifications(
                event.getPostAuthorId(),
                "작성하신 게시글에 새로운 댓글이 달렸습니다: " + event.getCommentContent(),
                event.getPostId(),
                NotificationType.COMMENT.name());

        saveAndSend(List.of(notification));
    }

    @Async
    @TransactionalEventListener
    public void handleClubJoinEvent(back.event.ClubJoinEvent event) {
        // 1. 가입자 본인에게 승인 알림
        Notifications welcomeNotification = new Notifications(
                event.getUserId(),
                "'" + event.getClubName() + "' 모임 가입이 승인되었습니다.",
                event.getClubId(),
                NotificationType.CLUB_WELCOME.name());
        saveAndSend(List.of(welcomeNotification));
    }

    @Async
    @TransactionalEventListener
    public void handleVoteDeadlineEvent(back.event.VoteDeadlineEvent event) {
        List<ClubMembers> members = clubMemberRepository.findByClubIdAndStatus(event.getClubId(),
                ClubMembers.Status.ACTIVE);

        List<Notifications> notifications = members.stream()
                .map(member -> new Notifications(
                        member.getUserId(),
                        "투표 '" + event.getVoteTitle() + "' 마감이 1시간 남았습니다.",
                        event.getVoteId(),
                        NotificationType.VOTE_DEADLINE.name()))
                .collect(Collectors.toList());

        saveAndSend(notifications);
    }

    private void saveAndSend(List<Notifications> notifications) {
        if (notifications.isEmpty())
            return;

        List<Notifications> savedNotifications = notificationsRepository.saveAll(notifications);

        for (Notifications notification : savedNotifications) {
            NotificationResponse response = NotificationResponse.from(notification);
            notificationService.send(notification.getUserId(), response);
        }
    }
}
