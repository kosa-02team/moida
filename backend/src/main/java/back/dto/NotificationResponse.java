package back.dto;

import back.domain.Notifications;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record NotificationResponse(Long id,
                                   String content,
                                   Long refId,
                                   String type,
                                   Boolean isRead,
                                   LocalDateTime createdAt,
                                   Long clubId  // 알림과 관련된 모임 ID (선택사항)
                                   ) {
    // Entity -> DTO 변환 메서드 (clubId 없이)
    public static NotificationResponse from(Notifications entity) {
        return NotificationResponse.builder()
                .id(entity.getNotiId())
                .content(entity.getContent())
                .refId(entity.getRefId())
                .type(entity.getRefType())
                .isRead(entity.getIsRead())
                .createdAt(entity.getCreatedAt())
                .clubId(null)
                .build();
    }

    // Entity -> DTO 변환 메서드 (clubId 포함)
    public static NotificationResponse from(Notifications entity, Long clubId) {
        return NotificationResponse.builder()
                .id(entity.getNotiId())
                .content(entity.getContent())
                .refId(entity.getRefId())
                .type(entity.getRefType())
                .isRead(entity.getIsRead())
                .createdAt(entity.getCreatedAt())
                .clubId(clubId)
                .build();
    }

}
