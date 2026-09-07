// src/main/java/com/example/ordersystem2/service/StockTransactionService.java
package com.example.ordersystem2.service;

import com.example.ordersystem2.model.StockTransaction;
import com.example.ordersystem2.repository.StockTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockTransactionService {

    @Autowired
    private StockTransactionRepository stockTransactionRepository;

    /**
     * 새로운 재고 트랜잭션을 기록합니다.
     * @param transaction 재고 트랜잭션 객체
     */
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

    /**
     * 모든 재고 트랜잭션을 조회합니다.
     * @return 모든 재고 트랜잭션 목록
     */
    public List<StockTransaction> getAllTransactions() {
        return stockTransactionRepository.findAll();
    }
}
