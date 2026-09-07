// src/main/java/com/example/ordersystem2/controller/OrderController.java
package com.example.ordersystem2.controller;

import com.example.ordersystem2.dto.OrderCreateDTO;
import com.example.ordersystem2.dto.OrderUpdateDTO;
import com.example.ordersystem2.exception.InsufficientInventoryException;
import com.example.ordersystem2.exception.OrderNotFoundException;
import com.example.ordersystem2.model.Order;
import com.example.ordersystem2.service.AccessControlService;
import com.example.ordersystem2.service.OrderService;
import com.example.ordersystem2.service.OrderTypeService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Controller
@RequestMapping("/orders") // 클래스 레벨 매핑 추가
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderTypeService orderTypeService;

    @Autowired
    private AccessControlService accessControlService;

    /**
     * 주문 관리 페이지를 표시합니다.
     */
    @GetMapping
    public String showOrders(HttpSession session, Model model) {
        if (!accessControlService.isLoggedIn(session)) {
            return accessControlService.redirectFor(session);
        }

        Long userId = (Long) session.getAttribute("userId");
        List<Order> orders = accessControlService.isHeadquarters(session)
                ? orderService.getAllOrders()
                : orderService.getOrdersByRequester(userId);
        model.addAttribute("orders", orders);
        model.addAttribute("orderTypes", orderTypeService.getAllOrderTypes());
        model.addAttribute("orderCreate", new OrderCreateDTO()); // "orderCreate"로 변경
        model.addAttribute("isStoreOwner", accessControlService.isStoreOwner(session));
        model.addAttribute("isHeadquarters", accessControlService.isHeadquarters(session));
        return "orders";
    }

    /**
     * 발주 생성 처리
     */
    @PostMapping
    public String createOrder(@ModelAttribute("orderCreate") @Valid OrderCreateDTO orderCreateDTO,
                              BindingResult bindingResult,
                              HttpSession session,
                              Model model) {
        if (accessControlService.isStoreOwner(session)) {
            Long userId = (Long) session.getAttribute("userId");
            if (bindingResult.hasErrors()) {
                logger.error("Binding errors: {}", bindingResult.getAllErrors());
                model.addAttribute("orders", orderService.getOrdersByRequester(userId));
                model.addAttribute("orderTypes", orderTypeService.getAllOrderTypes());
                model.addAttribute("isStoreOwner", true);
                model.addAttribute("isHeadquarters", false);
                return "orders"; // "orders.html" 템플릿으로 돌아감
            }
            try {
                logger.info("Creating new order: {}", orderCreateDTO);
                orderService.createOrder(orderCreateDTO, userId);
                return "redirect:/orders?success=request_created";
            } catch (InsufficientInventoryException e) {
                logger.error("Insufficient inventory: {}", e.getMessage());
                return "redirect:/orders?error=" + e.getMessage();
            } catch (Exception e) {
                logger.error("Unexpected error while creating order", e);
                return "redirect:/orders?error=unexpected_error";
            }
        }
        return accessControlService.redirectFor(session);
    }

    /**
     * 발주 수정 폼을 표시합니다.
     */
    @GetMapping("/edit/{id}")
    public String editOrderForm(@PathVariable("id") Long id, HttpSession session, Model model) {
        if (accessControlService.isStoreOwner(session)) {
            try {
                Long userId = (Long) session.getAttribute("userId");
                Order order = orderService.getOrderByIdForRequester(id, userId);
                if (!order.getStatus().canStoreOwnerEdit()) {
                    return "redirect:/orders?error=order_locked";
                }
                OrderUpdateDTO orderUpdate = new OrderUpdateDTO();
                orderUpdate.setOrderTypeId(order.getOrderType().getId());
                orderUpdate.setQuantity(order.getQuantity());
                model.addAttribute("order", order);
                model.addAttribute("orderUpdate", orderUpdate);
                model.addAttribute("orderTypes", orderTypeService.getAllOrderTypes());
                return "edit_order";
            } catch (OrderNotFoundException e) {
                logger.error("Order not found: {}", e.getMessage());
                return "redirect:/orders?error=order_not_found";
            }
        }
        return accessControlService.redirectFor(session);
    }

    /**
     * 발주 수정 처리
     */
    @PostMapping("/edit/{id}")
    public String editOrder(@PathVariable("id") Long id,
                            @ModelAttribute("orderUpdate") @Valid OrderUpdateDTO orderUpdate,
                            BindingResult bindingResult,
                            HttpSession session,
                            Model model) {
        if (accessControlService.isStoreOwner(session)) {
            if (bindingResult.hasErrors()) {
                logger.error("Binding errors: {}", bindingResult.getAllErrors());
                try {
                    model.addAttribute("order", orderService.getOrderByIdForRequester(id, (Long) session.getAttribute("userId")));
                } catch (OrderNotFoundException e) {
                    return "redirect:/orders?error=order_not_found";
                }
                model.addAttribute("orderTypes", orderTypeService.getAllOrderTypes());
                return "edit_order";
            }
            try {
                logger.info("Updating order ID: {}", id);
                logger.info("New Order Type ID: {}", orderUpdate.getOrderTypeId());
                logger.info("New Quantity: {}", orderUpdate.getQuantity());
                Long userId = (Long) session.getAttribute("userId");
                orderService.updateOrder(id, orderUpdate.getOrderTypeId(), orderUpdate.getQuantity(), userId);
                return "redirect:/orders?success=request_updated";
            } catch (IllegalStateException e) {
                logger.error("Order cannot be updated: {}", e.getMessage());
                return "redirect:/orders?error=order_locked";
            } catch (OrderNotFoundException | InsufficientInventoryException e) {
                logger.error("Error updating order: {}", e.getMessage());
                return "redirect:/orders/edit/" + id + "?error=" + e.getMessage();
            } catch (Exception e) {
                logger.error("Unexpected error while updating order", e);
                return "redirect:/orders/edit/" + id + "?error=unexpected_error";
            }
        }
        return accessControlService.redirectFor(session);
    }

    /**
     * 테스트 페이지를 표시합니다.
     */
    @GetMapping("/test")
    public String testPage(Model model) {
        return "redirect:/orders";
    }
}
