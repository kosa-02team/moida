package com.back.project.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vote_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoteOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_id")
    private Long optionId;

    @Column(name = "vote_id", nullable = false)
    private Long voteId;

    @Column(name = "option_text", nullable = false, length = 200)
    private String optionText;

    @Column(name = "option_order")
    @Builder.Default
    private Integer optionOrder = 1;

    @Column(name = "event_date")
    private LocalDateTime eventDate;

    @Column(length = 255)
    private String location;
}

