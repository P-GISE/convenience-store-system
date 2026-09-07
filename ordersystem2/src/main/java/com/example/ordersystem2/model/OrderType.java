// src/main/java/com/example/ordersystem2/model/OrderType.java
package com.example.ordersystem2.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "order_types")
@Data
@NoArgsConstructor
public class OrderType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // Order 엔티티와의 일대다 관계
    @OneToMany(mappedBy = "orderType", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Order> orders;

    // Inventory 엔티티와의 일대다 관계
    @OneToMany(mappedBy = "orderType", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Inventory> inventories;
}
