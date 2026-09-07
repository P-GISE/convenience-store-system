// src/main/java/com/example/ordersystem2/model/Order.java
package com.example.ordersystem2.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 발주 종류와의 다대일 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_type_id")
    private OrderType orderType;

    private Integer quantity;

    private LocalDateTime orderDate;

    // 주문 상태
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private LocalDateTime statusDate;

    // 기본 생성자
    public Order() {
        this.status = OrderStatus.RECEIVED;
        this.statusDate = LocalDateTime.now();
    }

    // 상태 변경 시 statusDate 업데이트
    public void setStatus(OrderStatus status) {
        this.status = status;
        this.statusDate = LocalDateTime.now();
    }
}
