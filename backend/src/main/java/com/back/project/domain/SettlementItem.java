package com.back.project.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "settlement_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "settlement_id", nullable = false)
    private Long settlementId;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "receipt_url", length = 255)
    private String receiptUrl;

    @Column(columnDefinition = "TEXT")
    private String description;
}

