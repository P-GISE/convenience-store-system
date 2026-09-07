// src/main/java/com/example/ordersystem2/repository/InventoryRepository.java
package com.example.ordersystem2.repository;

import com.example.ordersystem2.model.Inventory;
import com.example.ordersystem2.model.OrderType;
import com.example.ordersystem2.model.InventoryStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    @Override
    @EntityGraph(attributePaths = "orderType")
    List<Inventory> findAll();

    @Override
    @EntityGraph(attributePaths = "orderType")
    Optional<Inventory> findById(Long id);

    @EntityGraph(attributePaths = "orderType")
    Optional<Inventory> findByOrderType(OrderType orderType);

    @EntityGraph(attributePaths = "orderType")
    Optional<Inventory> findByOrderTypeAndStatus(OrderType orderType, InventoryStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "orderType")
    @Query("select i from Inventory i where i.orderType = :orderType and i.status = :status")
    Optional<Inventory> findByOrderTypeAndStatusForUpdate(@Param("orderType") OrderType orderType,
                                                          @Param("status") InventoryStatus status);

    @EntityGraph(attributePaths = "orderType")
    List<Inventory> findByStatus(InventoryStatus status);

    // 필요에 따라 다른 쿼리 메서드 추가
}
