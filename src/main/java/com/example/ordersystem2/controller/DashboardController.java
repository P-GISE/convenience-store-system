// src/main/java/com/example/ordersystem2/controller/DashboardController.java
package com.example.ordersystem2.controller;

import com.example.ordersystem2.model.Order;
import com.example.ordersystem2.model.OrderStatus;
import com.example.ordersystem2.service.AccessControlService;
import com.example.ordersystem2.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private AccessControlService accessControlService;

    /**
     * 대시보드 페이지 표시
     */
    @GetMapping
    public String showDashboard(HttpSession session, Model model) {
        if (!accessControlService.isHeadquarters(session)) {
            return accessControlService.redirectFor(session);
        }

        // 모든 주문 조회
        List<Order> allOrders = orderService.getAllOrders();
        model.addAttribute("orders", allOrders);

        // 상태별 주문 수 집계
        long receivedCount = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.RECEIVED).count();
        long preparationCount = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.PREPARATION).count();
        long departedCount = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.DEPARTED).count();
        long arrivedCount = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.ARRIVED).count();

        model.addAttribute("receivedCount", receivedCount);
        model.addAttribute("preparationCount", preparationCount);
        model.addAttribute("departedCount", departedCount);
        model.addAttribute("arrivedCount", arrivedCount);
        model.addAttribute("statuses", OrderStatus.values());

        return "dashboard";
    }

    /**
     * 주문 상태 업데이트 처리
     */
    @PostMapping("/updateStatus")
    public String updateOrderStatus(@RequestParam("orderId") Long orderId,
                                    @RequestParam("status") OrderStatus status,
                                    HttpSession session) {
        if (!accessControlService.isHeadquarters(session)) {
            return accessControlService.redirectFor(session);
        }

        try {
            orderService.updateOrderStatus(orderId, status);
            return "redirect:/dashboard?success";
        } catch (IllegalStateException e) {
            return "redirect:/dashboard?error=invalid_status_transition";
        } catch (Exception e) {
            return "redirect:/dashboard?error=unexpected_error";
        }
    }
}
