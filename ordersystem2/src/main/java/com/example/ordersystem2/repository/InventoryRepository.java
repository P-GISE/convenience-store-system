// src/main/java/com/example/ordersystem2/repository/InventoryRepository.java
package com.example.ordersystem2.repository;

import com.example.ordersystem2.model.Inventory;
import com.example.ordersystem2.model.OrderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByOrderType(OrderType orderType);
}
