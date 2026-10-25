package com.example.ordersystem2.service;

import com.example.ordersystem2.dto.CustomerOrderCreateDTO;
import com.example.ordersystem2.model.CustomerFulfillmentType;
import com.example.ordersystem2.model.CustomerOrder;
import com.example.ordersystem2.model.CustomerOrderStatus;
import com.example.ordersystem2.model.Inventory;
import com.example.ordersystem2.model.InventoryStatus;
import com.example.ordersystem2.model.OrderType;
import com.example.ordersystem2.model.User;
import com.example.ordersystem2.model.UserRole;
import com.example.ordersystem2.repository.CustomerOrderRepository;
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CustomerOrderServiceConcurrencyTest {

    @Autowired
    private CustomerOrderService customerOrderService;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderTypeRepository orderTypeRepository;

    @Autowired
    private StockTransactionRepository stockTransactionRepository;

    @Autowired
    private UserRepository userRepository;

    private User customer;
    private User owner;

    @BeforeEach
    void setUp() {
        clearData();
        customer = saveUser("concurrent-customer", UserRole.CUSTOMER);
        owner = saveUser("concurrent-owner", UserRole.STORE_OWNER);
    }

    @AfterEach
    void tearDown() {
        clearData();
    }

    @Test
    void concurrentCustomerOrdersCannotOversellInventory() throws Exception {
        OrderType product = saveProduct("동시 주문 상품");
        Inventory inventory = saveInventory(product, 2);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<?> first = executor.submit(() -> createAfter(start, product, 2, "010-1000-0001"));
        Future<?> second = executor.submit(() -> createAfter(start, product, 2, "010-1000-0002"));

        start.countDown();
        awaitIgnoringExpectedFailure(first);
        awaitIgnoringExpectedFailure(second);
        executor.shutdownNow();

        Inventory savedInventory = inventoryRepository.findById(inventory.getId()).orElseThrow();
        int reservedQuantity = customerOrderRepository.findAll().stream()
                .mapToInt(CustomerOrder::getQuantity)
                .sum();

        assertThat(savedInventory.getQuantity()).isZero();
        assertThat(reservedQuantity).isEqualTo(2);
        assertThat(customerOrderRepository.findAll()).hasSize(1);
    }

    @Test
    void concurrentCancellationRestoresReservedInventoryOnlyOnce() throws Exception {
        OrderType product = saveProduct("동시 취소 상품");
        Inventory inventory = saveInventory(product, 5);
        CustomerOrder order = customerOrderService.createCustomerOrder(customerOrderRequest(product, 2, "010-2000-0001"), customer.getId());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<?> first = executor.submit(() -> cancelAfter(start, order.getId()));
        Future<?> second = executor.submit(() -> cancelAfter(start, order.getId()));

        start.countDown();
        first.get(10, TimeUnit.SECONDS);
        second.get(10, TimeUnit.SECONDS);
        executor.shutdownNow();

        Inventory savedInventory = inventoryRepository.findById(inventory.getId()).orElseThrow();
        CustomerOrder savedOrder = customerOrderRepository.findById(order.getId()).orElseThrow();

        assertThat(savedInventory.getQuantity()).isEqualTo(5);
        assertThat(savedOrder.getStatus()).isEqualTo(CustomerOrderStatus.CANCELLED);
    }

    private void createAfter(CountDownLatch start, OrderType product, int quantity, String phone) {
        try {
            start.await(5, TimeUnit.SECONDS);
            customerOrderService.createCustomerOrder(customerOrderRequest(product, quantity, phone), customer.getId());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void cancelAfter(CountDownLatch start, Long orderId) {
        try {
            start.await(5, TimeUnit.SECONDS);
            customerOrderService.updateStatus(orderId, CustomerOrderStatus.CANCELLED, owner.getId());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void awaitIgnoringExpectedFailure(Future<?> future) throws Exception {
        try {
            future.get(10, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // One concurrent order is expected to fail after the first reserves the remaining stock.
        }
    }

    private void clearData() {
        customerOrderRepository.deleteAllInBatch();
        stockTransactionRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        inventoryRepository.deleteAllInBatch();
        orderTypeRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    private OrderType saveProduct(String name) {
        OrderType orderType = new OrderType();
        orderType.setName(name);
        orderType.setStoreOwner(owner);
        return orderTypeRepository.save(orderType);
    }

    private User saveUser(String username, UserRole role) {
        User user = new User(username, "synthetic-encoded");
        user.setRole(role);
        return userRepository.save(user);
    }

    private Inventory saveInventory(OrderType product, int quantity) {
        Inventory inventory = new Inventory();
        inventory.setOrderType(product);
        inventory.setQuantity(quantity);
        inventory.setStatus(InventoryStatus.ACTIVE);
        return inventoryRepository.save(inventory);
    }

    private CustomerOrderCreateDTO customerOrderRequest(OrderType product, int quantity, String phone) {
        CustomerOrderCreateDTO request = new CustomerOrderCreateDTO();
        request.setOrderTypeId(product.getId());
        request.setQuantity(quantity);
        request.setCustomerName("동시 고객");
        request.setCustomerPhone(phone);
        request.setFulfillmentType(CustomerFulfillmentType.PICKUP);
        return request;
    }
}
