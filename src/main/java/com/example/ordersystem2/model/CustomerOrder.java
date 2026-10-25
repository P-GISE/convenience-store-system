package com.example.ordersystem2.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_orders", indexes = {
        @Index(name = "idx_customer_order_status", columnList = "status"),
        @Index(name = "idx_customer_order_phone", columnList = "customer_phone")
})
@Data
@NoArgsConstructor
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_type_id", nullable = false)
    private OrderType orderType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_owner_id", nullable = false)
    private User storeOwner;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "customer_name", nullable = false, length = 80)
    private String customerName;

    @Column(name = "customer_phone", nullable = false, length = 40)
    private String customerPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerFulfillmentType fulfillmentType;

    @Column(length = 240)
    private String deliveryAddress;

    @Column(length = 240)
    private String requestMemo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerOrderStatus status = CustomerOrderStatus.RECEIVED;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime statusDate = LocalDateTime.now();

    public void setStatus(CustomerOrderStatus status) {
        this.status = status;
        this.statusDate = LocalDateTime.now();
    }
}
