package com.back.project.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementItems {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "settlement_id", nullable = false)
    private Long settlementId;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "receipt_url", length = 255)
    private String receiptUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    // 생성자
    public SettlementItems(Long settlementId, String itemName, BigDecimal amount, 
                           String receiptUrl, String description) {
        this.settlementId = settlementId;
        this.itemName = itemName;
        this.amount = amount;
        this.receiptUrl = receiptUrl;
        this.description = description;
    }

    // 도메인 메서드
    public void updateItem(String itemName, BigDecimal amount, String receiptUrl, String description) {
        this.itemName = itemName;
        this.amount = amount;
        this.receiptUrl = receiptUrl;
        this.description = description;
    }
}

