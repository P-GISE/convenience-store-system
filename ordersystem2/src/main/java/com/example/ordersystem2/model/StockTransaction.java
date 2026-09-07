// src/main/java/com/example/ordersystem2/model/StockTransaction.java
package com.example.ordersystem2.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_transactions")
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

    // 생성자
    public StockTransaction(Order order, OrderType orderType, Integer quantity) {
        this.order = order;
        this.orderType = orderType;
        this.quantity = quantity;
        this.transactionDate = LocalDateTime.now();
    }
}
