package com.back.project.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeePolicies extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Long policyId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "due_day")
    private Integer dueDay = 1;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // 생성자
    public FeePolicies(Long clubId, BigDecimal amount, Integer dueDay) {
        this.clubId = clubId;
        this.amount = amount;
        this.dueDay = dueDay;
    }

    // 도메인 메서드
    public void updatePolicy(BigDecimal amount, Integer dueDay) {
        this.amount = amount;
        this.dueDay = dueDay;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}

