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
                                   LocalDateTime createdAt
                                   ) {
    // Entity -> DTO 변환 메서드
    public static NotificationResponse from(Notifications entity) {
        return NotificationResponse.builder()
                .id(entity.getNotiId())
                .content(entity.getContent())
                .refId(entity.getRefId())
                .type(entity.getRefType())
                .isRead(entity.getIsRead())
                .createdAt(entity.getCreatedAt())
                .build();
    }

}
