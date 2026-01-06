package com.back.project.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "schedule_participants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "participant_id")
    private Long participantId;

    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "attendance_status", length = 20)
    @Builder.Default
    private String attendanceStatus = "UNDECIDED";

    @Column(name = "is_refunded")
    @Builder.Default
    private Boolean isRefunded = false;
}

