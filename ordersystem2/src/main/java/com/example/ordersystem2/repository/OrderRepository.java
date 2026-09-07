// src/main/java/com/example/ordersystem2/repository/OrderRepository.java
package com.example.ordersystem2.repository;

import com.example.ordersystem2.model.Order;
import com.example.ordersystem2.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // 상태별 주문 조회
    List<Order> findByStatus(OrderStatus status);

    // 여러 상태에 해당하는 주문 조회
    List<Order> findByStatusIn(List<OrderStatus> statuses);
}
