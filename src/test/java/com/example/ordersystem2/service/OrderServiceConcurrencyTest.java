package com.example.ordersystem2.service;

import com.example.ordersystem2.dto.OrderCreateDTO;
import com.example.ordersystem2.model.Inventory;
import com.example.ordersystem2.model.InventoryStatus;
import com.example.ordersystem2.model.Order;
import com.example.ordersystem2.model.OrderStatus;
import com.example.ordersystem2.model.OrderType;
import com.example.ordersystem2.repository.InventoryRepository;
import com.example.ordersystem2.repository.OrderRepository;
import com.example.ordersystem2.repository.OrderTypeRepository;
import com.example.ordersystem2.repository.StockTransactionRepository;
import com.example.ordersystem2.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderServiceConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderTypeRepository orderTypeRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private StockTransactionRepository stockTransactionRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        clearData();
    }

    @AfterEach
    void tearDown() {
        clearData();
    }

    @Test
    void concurrentArrivalSubmissionsIncreaseInventoryOnlyOnce() throws Exception {
        OrderType product = saveProduct("Concurrent Bottled Water");
        Inventory inventory = saveInventory(product, 10);
        orderService.createOrder(orderRequest(product, 5));
        Order order = orderRepository.findAll().get(0);

        orderService.updateOrderStatus(order.getId(), OrderStatus.PREPARATION);
        orderService.updateOrderStatus(order.getId(), OrderStatus.DEPARTED);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<?> first = executor.submit(() -> updateToArrivedAfter(start, order.getId()));
        Future<?> second = executor.submit(() -> updateToArrivedAfter(start, order.getId()));

        start.countDown();
        first.get(10, TimeUnit.SECONDS);
        second.get(10, TimeUnit.SECONDS);
        executor.shutdownNow();

        Inventory savedInventory = inventoryRepository.findById(inventory.getId()).orElseThrow();
        assertThat(savedInventory.getQuantity()).isEqualTo(15);
        assertThat(stockTransactionRepository.findByOrderId(order.getId())).hasSize(1);
    }

    private void updateToArrivedAfter(CountDownLatch start, Long orderId) {
        try {
            start.await(5, TimeUnit.SECONDS);
            orderService.updateOrderStatus(orderId, OrderStatus.ARRIVED);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void clearData() {
        stockTransactionRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        inventoryRepository.deleteAllInBatch();
        orderTypeRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    private OrderType saveProduct(String name) {
        OrderType orderType = new OrderType();
        orderType.setName(name);
        return orderTypeRepository.save(orderType);
    }

    private Inventory saveInventory(OrderType product, int quantity) {
        Inventory inventory = new Inventory();
        inventory.setOrderType(product);
        inventory.setQuantity(quantity);
        inventory.setStatus(InventoryStatus.ACTIVE);
        return inventoryRepository.save(inventory);
    }

    private OrderCreateDTO orderRequest(OrderType product, int quantity) {
        OrderCreateDTO request = new OrderCreateDTO();
        request.setOrderTypeId(product.getId());
        request.setQuantity(quantity);
        return request;
    }
}
