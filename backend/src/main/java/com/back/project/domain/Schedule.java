package com.back.project.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schedule {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(length = 255)
    private String location;

    @Column(name = "entry_fee")
    @Builder.Default
    private Long entryFee = 0L;

    @Column(name = "total_spent")
    @Builder.Default
    private Long totalSpent = 0L;

    @Column(name = "refund_per_person")
    @Builder.Default
    private Long refundPerPerson = 0L;

    @Column(length = 20)
    @Builder.Default
    private String status = "OPEN";

    @Column(name = "closed_at")
    private LocalDateTime closedAt;
}

