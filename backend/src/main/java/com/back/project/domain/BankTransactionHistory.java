package com.back.project.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "bank_transaction_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankTransactionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "bank_transaction_at", nullable = false)
    private LocalDateTime bankTransactionAt;

    @Column(name = "sender_account_number", nullable = false, length = 255)
    private String senderAccountNumber;

    @Column(name = "sender_name", nullable = false, length = 50)
    private String senderName;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "is_matched")
    @Builder.Default
    private Boolean isMatched = false;

    @Column(name = "unique_tx_key", unique = true, length = 255)
    private String uniqueTxKey;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

