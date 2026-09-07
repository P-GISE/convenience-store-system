// src/main/java/com/example/ordersystem2/controller/StockTransactionController.java
package com.example.ordersystem2.controller;

import com.example.ordersystem2.model.StockTransaction;
import com.example.ordersystem2.service.StockTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


import java.util.List;

@Controller
public class StockTransactionController {

    @Autowired
    private StockTransactionService stockTransactionService;

    /**
     * 재고 트랜잭션 내역 페이지를 표시합니다.
     */
    @GetMapping("/stock-transactions")
    public String showStockTransactions(Model model) {
        List<StockTransaction> transactions = stockTransactionService.getAllTransactions();
        model.addAttribute("transactions", transactions);
        return "stock_transactions";
    }
}
