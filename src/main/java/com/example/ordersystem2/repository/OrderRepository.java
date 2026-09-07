// src/main/java/com/example/ordersystem2/repository/OrderRepository.java
package com.example.ordersystem2.repository;

import com.example.ordersystem2.model.Order;
import com.example.ordersystem2.model.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Override
    @EntityGraph(attributePaths = {"orderType", "requester"})
    List<Order> findAll();

    @Override
    @EntityGraph(attributePaths = {"orderType", "requester"})
    Optional<Order> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"orderType", "requester"})
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForStatusUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"orderType", "requester"})
    List<Order> findByRequesterId(Long requesterId);

    // 상태별 주문 조회
    @EntityGraph(attributePaths = {"orderType", "requester"})
    List<Order> findByStatus(OrderStatus status);

    // 여러 상태에 해당하는 주문 조회
    @EntityGraph(attributePaths = {"orderType", "requester"})
    List<Order> findByStatusIn(List<OrderStatus> statuses);
}
