// src/main/java/com/example/ordersystem2/repository/OrderTypeRepository.java
package com.example.ordersystem2.repository;

import com.example.ordersystem2.model.OrderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderTypeRepository extends JpaRepository<OrderType, Long> {
}
