package back.service.notifications;

import back.domain.Notifications;
import back.domain.post.Posts;
import back.domain.schedule.Schedules;
import back.domain.vote.Votes;
import back.dto.NotificationResponse;
import back.repository.notifications.EmitterRepository;
import back.repository.notifications.NotificationsRepository;
import back.repository.post.PostRepository;
import back.repository.schedule.ScheduleRepository;
import back.repository.vote.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final EmitterRepository emitterRepository;
    private final NotificationsRepository notificationsRepository;
    private final ScheduleRepository scheduleRepository;
    private final PostRepository postRepository;
    private final VoteRepository voteRepository;

    //구독 (Subscribe)
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(60L * 60 * 1000);
        String key = userId + "_" + System.currentTimeMillis();

        emitter.onCompletion(() -> emitterRepository.deleteById(key));
        emitter.onTimeout(() -> emitterRepository.deleteById(key));

        try {
            emitter.send(SseEmitter.event().name("test").data("success"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return emitterRepository.save(key, emitter);
    }

    //전송 (Send)
    public void send(Long userId, Object data) {
        Map<String, SseEmitter> emitters = emitterRepository.findAllEmitterStartWithByUserId(userId);

        emitters.forEach((key, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification") // 클라이언트에서 수신할 이벤트 이름
                        .data(data));
            } catch (IOException e) {
                emitterRepository.deleteById(key);
            }
        });
    }

    /**
     * 알림 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Long userId, Pageable pageable, Boolean isRead) {
        Page<Notifications> notifications;
        if (isRead != null) {
            notifications = notificationsRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, isRead, pageable);
        } else {
            notifications = notificationsRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        // clubId를 포함한 NotificationResponse로 변환
        return notifications.map(notification -> {
            Long clubId = getClubIdFromNotification(notification);
            return NotificationResponse.from(notification, clubId);
        });
    }

    /**
     * 알림에서 clubId 추출
     * @param notification 알림 엔티티
     * @return clubId (없으면 null)
     */
    public Long getClubIdFromNotification(Notifications notification) {
        if (notification.getRefId() == null || notification.getRefType() == null) {
            return null;
        }

        try {
            switch (notification.getRefType()) {
                case "SCHEDULE": {
                    return scheduleRepository.findById(notification.getRefId())
                            .map(Schedules::getClubId)
                            .orElse(null);
                }
                case "POST":
                case "COMMENT": {
                    return postRepository.findById(notification.getRefId())
                            .map(Posts::getClub)
                            .map(back.domain.club.Clubs::getClubId)
                            .orElse(null);
                }
                case "VOTE_DEADLINE": {
                    return voteRepository.findById(notification.getRefId())
                            .map(vote -> {
                                // ATTENDANCE 타입: scheduleId로부터 clubId 조회
                                if ("ATTENDANCE".equals(vote.getVoteType()) && vote.getScheduleId() != null) {
                                    return scheduleRepository.findById(vote.getScheduleId())
                                            .map(Schedules::getClubId)
                                            .orElse(null);
                                }
                                // GENERAL 타입: postId로부터 clubId 조회
                                else if ("GENERAL".equals(vote.getVoteType()) && vote.getPostId() != null) {
                                    return postRepository.findById(vote.getPostId())
                                            .map(Posts::getClub)
                                            .map(back.domain.club.Clubs::getClubId)
                                            .orElse(null);
                                }
                                return null;
                            })
                            .orElse(null);
                }
                case "CLUB_WELCOME": {
                    // refId가 clubId인 경우
                    return notification.getRefId();
                }
                default:
                    return null;
            }
        } catch (Exception e) {
            // 조회 실패 시 null 반환 (로그 남기고 계속 진행)
            log.warn("알림에서 clubId 추출 실패: notificationId={}, refType={}, refId={}, error={}",
                    notification.getNotiId(), notification.getRefType(), notification.getRefId(), e.getMessage());
            return null;
        }
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notifications notification = notificationsRepository.findByNotiIdAndUserId(notificationId, userId)
                .orElseThrow(back.exception.ResourceException.NotFound::new);
        notification.markAsRead();
        notificationsRepository.save(notification);
    }

    /**
     * 알림 삭제
     */
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notifications notification = notificationsRepository.findByNotiIdAndUserId(notificationId, userId)
                .orElseThrow(back.exception.ResourceException.NotFound::new);
        notificationsRepository.delete(notification);
    }

    /**
     * 읽지 않은 알림 개수 조회
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationsRepository.countByUserIdAndIsReadFalse(userId);
    }
}