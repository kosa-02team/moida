package com.back.project.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberActionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "action_id")
    private Long actionId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "action_type", nullable = false, length = 30)
    private String actionType;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @CreationTimestamp
    @Column(name = "action_at", updatable = false)
    private LocalDateTime actionAt;

    // 생성자
    public MemberActionHistory(Long clubId, Long userId, Long reportId, 
                               String actionType, Long actorId, String reason) {
        this.clubId = clubId;
        this.userId = userId;
        this.reportId = reportId;
        this.actionType = actionType;
        this.actorId = actorId;
        this.reason = reason;
    }
}
