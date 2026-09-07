// src/main/java/com/example/ordersystem2/model/StockTransaction.java
package com.example.ordersystem2.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_transactions", indexes = {
        @Index(name = "idx_dispose_time", columnList = "dispose_time"),
        @Index(name = "idx_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_stock_transactions_order", columnNames = "order_id")
})
@Data
@NoArgsConstructor
public class StockTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 관련된 주문과의 다대일 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // 발주 종류와의 다대일 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_type_id", nullable = false)
    private OrderType orderType;

    private Integer quantity;

    private LocalDateTime transactionDate;

    // 폐기 시간
    @Column(name = "dispose_time")
    private LocalDateTime disposeTime;

    // 거래 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    // 직접 Inventory 참조 추가
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    // 생성자
    public StockTransaction(Order order, OrderType orderType, Integer quantity, LocalDateTime disposeTime, Inventory inventory) {
        this.order = order;
        this.orderType = orderType;
        this.quantity = quantity;
        this.transactionDate = LocalDateTime.now();
        this.disposeTime = disposeTime;
        this.status = TransactionStatus.PENDING;
        this.inventory = inventory;
    }
}
