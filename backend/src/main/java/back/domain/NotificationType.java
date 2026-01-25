package back.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationType {
    SCHEDULE("일정"),
    POST("게시글"),
    COMMENT("댓글"),
    CLUB_WELCOME("모임 가입 환영"),
    CLUB_JOIN_REQUEST("모임 가입 신청"),
    VOTE_DEADLINE("투표 마감 임박"),
    PAYMENT_REQUEST("결제 요청");

    private final String description;
}
