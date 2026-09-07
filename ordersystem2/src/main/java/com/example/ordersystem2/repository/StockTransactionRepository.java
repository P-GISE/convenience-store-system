// src/main/java/com/example/ordersystem2/repository/StockTransactionRepository.java
package com.example.ordersystem2.repository;

import com.example.ordersystem2.model.StockTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {

    // 특정 주문에 대한 모든 트랜잭션 조회
    List<StockTransaction> findByOrderId(Long orderId);
}
