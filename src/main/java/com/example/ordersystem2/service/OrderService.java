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
import com.example.ordersystem2.model.TransactionStatus;
import com.example.ordersystem2.model.User;
import com.example.ordersystem2.repository.OrderRepository;
import com.example.ordersystem2.repository.OrderTypeRepository;
import com.example.ordersystem2.repository.UserRepository;
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

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public List<Order> getAllOrders() {
        logger.info("Fetching all orders");
        return orderRepository.findAll();
    }

    @Transactional
    public List<Order> getOrdersByRequester(Long requesterId) {
        logger.info("Fetching orders for requester ID: {}", requesterId);
        return orderRepository.findByRequesterId(requesterId);
    }

    @Transactional
    public void createOrder(OrderCreateDTO orderCreateDTO) throws InsufficientInventoryException, OrderNotFoundException, InventoryNotFoundException {
        createOrder(orderCreateDTO, null);
    }

    @Transactional
    public void createOrder(OrderCreateDTO orderCreateDTO, Long requesterId) throws InsufficientInventoryException, OrderNotFoundException, InventoryNotFoundException {
        logger.info("Creating order: {}", orderCreateDTO);
        OrderType orderType = orderTypeRepository.findById(orderCreateDTO.getOrderTypeId())
                .orElseThrow(() -> new OrderNotFoundException("발주 종류를 찾을 수 없습니다."));

        Order order = new Order();
        order.setOrderType(orderType);
        order.setQuantity(orderCreateDTO.getQuantity());
        order.setOrderDate(LocalDateTime.now());
        if (requesterId != null) {
            User requester = userRepository.findById(requesterId)
                    .orElseThrow(() -> new OrderNotFoundException("요청 사용자를 찾을 수 없습니다."));
            order.setRequester(requester);
        }

        orderRepository.save(order);
        logger.info("Order created successfully. Order ID: {}", order.getId());
    }

    public Order getOrderById(Long id) throws OrderNotFoundException {
        logger.info("Fetching order by ID: {}", id);
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다."));
    }

    public Order getOrderByIdForRequester(Long id, Long requesterId) throws OrderNotFoundException {
        Order order = getOrderById(id);
        if (order.getRequester() == null || !order.getRequester().getId().equals(requesterId)) {
            throw new OrderNotFoundException("요청을 찾을 수 없습니다.");
        }
        return order;
    }

    @Transactional
    public void updateOrder(Long id, Long orderTypeId, Integer quantity) throws OrderNotFoundException, InsufficientInventoryException, InventoryNotFoundException {
        updateOrder(id, orderTypeId, quantity, null);
    }

    @Transactional
    public void updateOrder(Long id, Long orderTypeId, Integer quantity, Long requesterId) throws OrderNotFoundException, InsufficientInventoryException, InventoryNotFoundException {
        logger.info("Updating order ID: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("발주를 찾을 수 없습니다."));

        if (requesterId != null && (order.getRequester() == null || !order.getRequester().getId().equals(requesterId))) {
            throw new OrderNotFoundException("요청을 찾을 수 없습니다.");
        }

        if (!order.getStatus().canStoreOwnerEdit()) {
            throw new IllegalStateException("본사 처리가 시작된 요청은 수정할 수 없습니다.");
        }

        OrderType newOrderType = orderTypeRepository.findById(orderTypeId)
                .orElseThrow(() -> new OrderNotFoundException("발주 종류를 찾을 수 없습니다."));

        order.setOrderType(newOrderType);
        order.setQuantity(quantity);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.RECEIVED); // 상태를 초기 상태로 리셋 (필요에 따라 조정)
        orderRepository.save(order);
        logger.info("Order updated successfully. Order ID: {}", order.getId());
    }

    /**
     * 주문 상태 업데이트
     */
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus) throws OrderNotFoundException, InventoryNotFoundException {
        logger.info("Updating status for Order ID: {} to {}", orderId, newStatus);
        Order order = orderRepository.findByIdForStatusUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다."));

        OrderStatus previousStatus = order.getStatus();
        if (!previousStatus.canTransitionTo(newStatus)) {
            throw new IllegalStateException("요청 상태는 이전 단계로 되돌리거나 단계를 건너뛸 수 없습니다.");
        }

        order.setStatus(newStatus);
        order.setStatusDate(LocalDateTime.now());

        // 상태가 ARRIVED인 경우 재고 및 트랜잭션 업데이트
        if (newStatus == OrderStatus.ARRIVED
                && previousStatus != OrderStatus.ARRIVED
                && !stockTransactionService.hasTransactionForOrder(orderId)) {
            OrderType orderType = order.getOrderType();
            Integer quantity = order.getQuantity();

            // 해당 발주 종류의 재고 조회
            Inventory inventory = inventoryService.getOrCreateInventoryByOrderType(orderType);
            // 재고 수량 증가
            inventory.setQuantity(inventory.getQuantity() + quantity);
            inventoryService.saveInventory(inventory);
            logger.info("Inventory updated upon arrival: OrderType ID {}, New Quantity {}", orderType.getId(), inventory.getQuantity());

            // 재고 트랜잭션 기록
            // 올바른 생성자 사용: (Order, OrderType, Integer, LocalDateTime, Inventory)
            StockTransaction transaction = new StockTransaction(order, orderType, quantity, LocalDateTime.now(), inventory);
            transaction.setStatus(TransactionStatus.COMPLETED);
            stockTransactionService.recordTransaction(transaction);
            logger.info("Stock transaction recorded. Transaction ID: {}", transaction.getId());
        }

        orderRepository.save(order);
        logger.info("Order status updated successfully. Order ID: {}", order.getId());
    }

    /**
     * 대시보드용 주문 조회
     */
    public List<Order> getOrdersByStatuses(List<OrderStatus> statuses) {
        logger.info("Fetching orders with statuses: {}", statuses);
        return orderRepository.findByStatusIn(statuses);
    }
}
