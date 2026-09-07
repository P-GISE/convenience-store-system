// src/main/java/com/example/ordersystem2/repository/StockTransactionRepository.java
package com.example.ordersystem2.repository;

import com.example.ordersystem2.model.StockTransaction;
import com.example.ordersystem2.model.TransactionStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {
    @Override
    @EntityGraph(attributePaths = {"order", "orderType", "inventory"})
    List<StockTransaction> findAll();

    @Override
    @EntityGraph(attributePaths = {"order", "orderType", "inventory"})
    Optional<StockTransaction> findById(Long id);

    /**
     * 특정 주문 ID에 해당하는 재고 트랜잭션을 조회합니다.
     * @param orderId 주문 ID
     * @return 재고 트랜잭션 목록
     */
    @EntityGraph(attributePaths = {"order", "orderType", "inventory"})
    List<StockTransaction> findByOrderId(Long orderId);

    /**
     * 특정 Inventory ID에 해당하는 재고 트랜잭션을 조회합니다.
     * @param inventoryId 재고 ID
     * @return 재고 트랜잭션 목록
     */
    @EntityGraph(attributePaths = {"order", "orderType", "inventory"})
    List<StockTransaction> findByInventoryId(Long inventoryId);

    /**
     * 특정 상태의 재고 트랜잭션을 조회합니다.
     * @param status 거래 상태
     * @return 재고 트랜잭션 목록
     */
    @EntityGraph(attributePaths = {"order", "orderType", "inventory"})
    List<StockTransaction> findByStatus(TransactionStatus status);

    /**
     * 현재 시간보다 이전의 disposeTime을 가지며 특정 상태인 재고 거래 목록을 조회합니다.
     * @param now 현재 시간
     * @param status 거래 상태
     * @return 재고 거래 목록
     */
    @EntityGraph(attributePaths = {"order", "orderType", "inventory"})
    List<StockTransaction> findByDisposeTimeBeforeAndStatus(LocalDateTime now, TransactionStatus status);
}
