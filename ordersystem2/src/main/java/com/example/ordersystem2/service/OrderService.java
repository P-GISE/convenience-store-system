// src/main/java/com/example/ordersystem2/service/OrderService.java
package com.example.ordersystem2.service;

import com.example.ordersystem2.dto.OrderCreateDTO;
import com.example.ordersystem2.exception.InsufficientInventoryException;
import com.example.ordersystem2.exception.InventoryNotFoundException;
import com.example.ordersystem2.exception.OrderNotFoundException;
import com.example.ordersystem2.model.Inventory;
import com.example.ordersystem2.model.Order;
import com.example.ordersystem2.model.OrderStatus;
import com.example.ordersystem2.model.OrderType;
import com.example.ordersystem2.model.StockTransaction;
import com.example.ordersystem2.repository.OrderRepository;
import com.example.ordersystem2.repository.OrderTypeRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderTypeRepository orderTypeRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private StockTransactionService stockTransactionService;

    public List<Order> getAllOrders() {
        logger.info("Fetching all orders");
        return orderRepository.findAll();
    }

    public void createOrder(OrderCreateDTO orderCreateDTO) throws InsufficientInventoryException, OrderNotFoundException, InventoryNotFoundException {
        logger.info("Creating order: {}", orderCreateDTO);
        OrderType orderType = orderTypeRepository.findById(orderCreateDTO.getOrderTypeId())
                .orElseThrow(() -> new OrderNotFoundException("발주 종류를 찾을 수 없습니다."));

        // 재고 확인 및 업데이트
        Inventory inventory = inventoryService.getInventoryByOrderType(orderType);

        if (inventory.getQuantity() < orderCreateDTO.getQuantity()) {
            logger.error("Insufficient inventory for OrderType ID: {}", orderType.getId());
            throw new InsufficientInventoryException("재고가 부족합니다.");
        }

        inventory.setQuantity(inventory.getQuantity() - orderCreateDTO.getQuantity());
        inventoryService.saveInventory(inventory); // 재고 저장
        logger.info("Inventory updated: OrderType ID {}, New Quantity {}", orderType.getId(), inventory.getQuantity());

        // 주문 생성
        Order order = new Order();
        order.setOrderType(orderType);
        order.setQuantity(orderCreateDTO.getQuantity());
        order.setOrderDate(LocalDateTime.now());

        orderRepository.save(order);
        logger.info("Order created successfully: {}", order);
    }

    public Order getOrderById(Long id) throws OrderNotFoundException {
        logger.info("Fetching order by ID: {}", id);
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다."));
    }

    @Transactional
    public void updateOrder(Long id, Long orderTypeId, Integer quantity) throws OrderNotFoundException, InsufficientInventoryException, InventoryNotFoundException {
        logger.info("Updating order ID: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("발주를 찾을 수 없습니다."));

        OrderType newOrderType = orderTypeRepository.findById(orderTypeId)
                .orElseThrow(() -> new OrderNotFoundException("발주 종류를 찾을 수 없습니다."));

        // 이전 재고 복구
        Inventory previousInventory = inventoryService.getInventoryByOrderType(order.getOrderType());
        previousInventory.setQuantity(previousInventory.getQuantity() + order.getQuantity());
        inventoryService.saveInventory(previousInventory); // 이전 재고 저장
        logger.info("Previous inventory restored: OrderType ID {}, New Quantity {}", previousInventory.getOrderType().getId(), previousInventory.getQuantity());

        // 새로운 재고 확인 및 업데이트
        Inventory newInventory = inventoryService.getInventoryByOrderType(newOrderType);
        if (newInventory.getQuantity() < quantity) {
            logger.error("Insufficient inventory for OrderType ID: {}", newOrderType.getId());
            throw new InsufficientInventoryException("재고가 부족합니다.");
        }

        newInventory.setQuantity(newInventory.getQuantity() - quantity);
        inventoryService.saveInventory(newInventory); // 새로운 재고 저장
        logger.info("New inventory updated: OrderType ID {}, New Quantity {}", newOrderType.getId(), newInventory.getQuantity());

        // 주문 업데이트
        order.setOrderType(newOrderType);
        order.setQuantity(quantity);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.RECEIVED); // 상태를 초기 상태로 리셋 (필요에 따라 조정)
        orderRepository.save(order);
        logger.info("Order updated successfully: {}", order);
    }

    /**
     * 주문 상태 업데이트
     */
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus) throws OrderNotFoundException, InventoryNotFoundException {
        logger.info("Updating status for Order ID: {} to {}", orderId, newStatus);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다."));

        order.setStatus(newStatus);
        order.setStatusDate(LocalDateTime.now());

        // 상태가 ARRIVED인 경우 재고 및 트랜잭션 업데이트
        if (newStatus == OrderStatus.ARRIVED) {
            OrderType orderType = order.getOrderType();
            Integer quantity = order.getQuantity();

            // 해당 발주 종류의 재고 조회
            Inventory inventory = inventoryService.getInventoryByOrderType(orderType);
            // 재고 수량 증가
            inventory.setQuantity(inventory.getQuantity() + quantity);
            inventoryService.saveInventory(inventory);
            logger.info("Inventory updated upon arrival: OrderType ID {}, New Quantity {}", orderType.getId(), inventory.getQuantity());

            // 재고 트랜잭션 기록
            StockTransaction transaction = new StockTransaction(order, orderType, quantity);
            stockTransactionService.recordTransaction(transaction);
            logger.info("Stock transaction recorded: {}", transaction);
        }

        orderRepository.save(order);
        logger.info("Order status updated successfully: {}", order);
    }

    /**
     * 대시보드용 주문 조회
     */
    public List<Order> getOrdersByStatuses(List<OrderStatus> statuses) {
        logger.info("Fetching orders with statuses: {}", statuses);
        return orderRepository.findByStatusIn(statuses);
    }
}
