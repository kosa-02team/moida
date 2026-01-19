package back.listener;

import back.domain.Notifications;
import back.dto.NotificationResponse;
import back.event.ScheduleRegisteredEvent;
import back.repository.club.ClubMemberRepository;
import back.repository.notifications.NotificationsRepository;
import back.service.notifications.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final ClubMemberRepository clubMemberRepository;
    private final NotificationsRepository notificationsRepository;
    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener
    public void handleScheduleRegisteredEvent(ScheduleRegisteredEvent scheduleRegisteredEvent) {
        //TODO : 활성화 상태인 멤버 리스트를 조회하는 로직으로 교체 필요
        //더미 데이터
        List<Long> memberIds = List.of(1L, 2L, 3L);

        List<Notifications> list = new ArrayList<>();
        for (int i = 0; i < memberIds.size(); i++) {
            list.add(new Notifications(
                    memberIds.get(i),
                    "새로운 일정 '" + scheduleRegisteredEvent.getScheduleName() + "'이 등록 되었습니다.",
                    scheduleRegisteredEvent.getScheduleId(),
                    "SCHEDULE"
                    ));
        }
        List<Notifications> notifications = notificationsRepository.saveAll(list);

        for (Notifications notification : notifications) {
            NotificationResponse response = NotificationResponse.from(notification);
            notificationService.send(notification.getUserId(), response);
        }


    }
}
