package back.controller.notifications;

import back.common.response.SuccessResponse;
import back.config.security.UserPrincipal;
import back.dto.NotificationResponse;
import back.service.notifications.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * SSE 알림 구독
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return notificationService.subscribe(userPrincipal.getUserId());
    }

    /**
     * 알림 목록 조회 (페이징)
     * GET /api/notifications?page=0&size=20&isRead=false
     */
    @GetMapping
    public SuccessResponse<Page<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean isRead) {
        Long userId = userPrincipal.getUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponse> notifications = notificationService.getNotifications(userId, pageable, isRead);
        return SuccessResponse.success(HttpStatus.OK, notifications);
    }

    /**
     * 읽지 않은 알림 개수 조회
     * GET /api/notifications/unread-count
     */
    @GetMapping("/unread-count")
    public SuccessResponse<Long> getUnreadCount(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getUserId();
        long count = notificationService.getUnreadCount(userId);
        return SuccessResponse.success(HttpStatus.OK, count);
    }

    /**
     * 알림 읽음 처리
     * PUT /api/notifications/{notificationId}/read
     */
    @PutMapping("/{notificationId}/read")
    public SuccessResponse<Void> markAsRead(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long notificationId) {
        Long userId = userPrincipal.getUserId();
        notificationService.markAsRead(notificationId, userId);
        return SuccessResponse.success(HttpStatus.OK);
    }

    /**
     * 알림 삭제
     * DELETE /api/notifications/{notificationId}
     */
    @DeleteMapping("/{notificationId}")
    public SuccessResponse<Void> deleteNotification(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long notificationId) {
        Long userId = userPrincipal.getUserId();
        notificationService.deleteNotification(notificationId, userId);
        return SuccessResponse.success(HttpStatus.OK);
    }
}
