package com.back.project.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
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

    @OneToOne
    @MapsId
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(length = 255)
    private String location;

    @Column(name = "entry_fee", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal entryFee = BigDecimal.ZERO;

    @Column(name = "total_spent", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalSpent = BigDecimal.ZERO;

    @Column(name = "refund_per_person", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal refundPerPerson = BigDecimal.ZERO;

    @Column(length = 20)
    @Builder.Default
    private String status = "OPEN";

    @Column(name = "closed_at")
    private LocalDateTime closedAt;
}

