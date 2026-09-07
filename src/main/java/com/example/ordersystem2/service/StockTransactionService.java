// src/main/java/com/example/ordersystem2/service/StockTransactionService.java
package com.example.ordersystem2.service;

import com.example.ordersystem2.exception.InventoryNotFoundException;
import com.example.ordersystem2.model.StockTransaction;
import com.example.ordersystem2.model.TransactionStatus;
import com.example.ordersystem2.repository.StockTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StockTransactionService {

    @Autowired
    private StockTransactionRepository stockTransactionRepository;

    /**
     * 새로운 재고 트랜잭션을 기록합니다.
     * @param transaction 재고 트랜잭션 객체
     */
    @Transactional
    public void recordTransaction(StockTransaction transaction) {
        stockTransactionRepository.save(transaction);
    }

    /**
     * 특정 주문에 대한 모든 재고 트랜잭션을 조회합니다.
     * @param orderId 주문 ID
     * @return 재고 트랜잭션 목록
     */
    public List<StockTransaction> getTransactionsByOrderId(Long orderId) {
        return stockTransactionRepository.findByOrderId(orderId);
    }

    public boolean hasTransactionForOrder(Long orderId) {
        return !stockTransactionRepository.findByOrderId(orderId).isEmpty();
    }

    /**
     * 특정 Inventory에 대한 모든 재고 트랜잭션을 조회합니다.
     * @param inventoryId 재고 ID
     * @return 재고 트랜잭션 목록
     */
    public List<StockTransaction> getTransactionsByInventoryId(Long inventoryId) {
        return stockTransactionRepository.findByInventoryId(inventoryId);
    }

    /**
     * 특정 상태의 모든 재고 트랜잭션을 조회합니다.
     * @param status 거래 상태
     * @return 재고 트랜잭션 목록
     */
    public List<StockTransaction> getTransactionsByStatus(TransactionStatus status) {
        return stockTransactionRepository.findByStatus(status);
    }

    /**
     * 자동 폐기 처리를 위해 특정 시간 이전에 disposeTime을 가진 PENDING 상태의 재고 트랜잭션을 조회합니다.
     * @param now 현재 시간
     * @return 폐기 대상 재고 트랜잭션 목록
     */
    public List<StockTransaction> getPendingTransactionsBefore(LocalDateTime now) {
        return stockTransactionRepository.findByDisposeTimeBeforeAndStatus(now, TransactionStatus.PENDING);
    }

    /**
     * 특정 재고 트랜잭션을 ID로 조회합니다.
     * @param id 트랜잭션 ID
     * @return StockTransaction 객체
     * @throws InventoryNotFoundException 트랜잭션을 찾을 수 없는 경우
     */
    public StockTransaction getTransactionById(Long id) throws InventoryNotFoundException {
        return stockTransactionRepository.findById(id)
                .orElseThrow(() -> new InventoryNotFoundException("거래를 찾을 수 없습니다. ID: " + id));
    }

    /**
     * 특정 재고 거래를 폐기 처리합니다.
     * @param transaction 폐기할 재고 거래
     */
    @Transactional
    public void disposeTransaction(StockTransaction transaction) {
        transaction.setStatus(TransactionStatus.DISPOSED);
        stockTransactionRepository.save(transaction);
    }

    /**
     * 재고 트랜잭션을 상태별로 업데이트합니다.
     * @param transactionId 거래 ID
     * @param status 업데이트할 상태
     * @throws InventoryNotFoundException 거래를 찾을 수 없는 경우
     */
    @Transactional
    public void updateTransactionStatus(Long transactionId, TransactionStatus status) throws InventoryNotFoundException {
        StockTransaction transaction = stockTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new InventoryNotFoundException("거래를 찾을 수 없습니다. ID: " + transactionId));
        transaction.setStatus(status);
        stockTransactionRepository.save(transaction);
    }

    /**
     * 모든 재고 트랜잭션을 조회합니다.
     * @return 모든 재고 트랜잭션 목록
     */
    public List<StockTransaction> getAllTransactions() {
        return stockTransactionRepository.findAll();
    }
}
