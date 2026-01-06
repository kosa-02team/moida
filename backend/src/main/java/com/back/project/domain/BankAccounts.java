package com.back.project.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BankAccounts extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "bank_name", nullable = false, length = 50)
    private String bankName;

    @Column(name = "account_number", nullable = false, unique = true, length = 255)
    private String accountNumber;

    @Column(name = "depositor_name", nullable = false, length = 50)
    private String depositorName;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 생성자
    public BankAccounts(Long userId, String bankName, String accountNumber, String depositorName) {
        this.userId = userId;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.depositorName = depositorName;
    }

    // 도메인 메서드
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void updateAccountInfo(String bankName, String accountNumber, String depositorName) {
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.depositorName = depositorName;
    }
}

