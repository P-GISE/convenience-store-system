package com.example.ordersystem2.service;

import com.example.ordersystem2.dto.OrderCreateDTO;
import com.example.ordersystem2.exception.OrderNotFoundException;
import com.example.ordersystem2.model.Inventory;
import com.example.ordersystem2.model.InventoryStatus;
import com.example.ordersystem2.model.Order;
import com.example.ordersystem2.model.OrderStatus;
import com.example.ordersystem2.model.OrderType;
import com.example.ordersystem2.model.StockTransaction;
import com.example.ordersystem2.model.TransactionStatus;
import com.example.ordersystem2.model.User;
import com.example.ordersystem2.model.UserRole;
import com.example.ordersystem2.repository.InventoryRepository;
import com.example.ordersystem2.repository.OrderRepository;
import com.example.ordersystem2.repository.OrderTypeRepository;
import com.example.ordersystem2.repository.StockTransactionRepository;
import com.example.ordersystem2.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class OrderServiceTest {

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

    @Test
    void createOrderDoesNotDecreaseInventory() throws Exception {
        OrderType product = saveProduct("Cup Noodles");
        Inventory inventory = saveInventory(product, 10);
        OrderCreateDTO request = orderRequest(product, 5);

        orderService.createOrder(request);

        Inventory savedInventory = inventoryRepository.findById(inventory.getId()).orElseThrow();
        List<Order> orders = orderRepository.findAll();
        assertThat(savedInventory.getQuantity()).isEqualTo(10);
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getStatus()).isEqualTo(OrderStatus.RECEIVED);
    }

    @Test
    void arrivedOrderIncreasesInventoryOnceAndRecordsOneCompletedTransaction() throws Exception {
        OrderType product = saveProduct("Bottled Water");
        Inventory inventory = saveInventory(product, 10);
        OrderCreateDTO request = orderRequest(product, 5);
        orderService.createOrder(request);
        Order order = orderRepository.findAll().get(0);

        orderService.updateOrderStatus(order.getId(), OrderStatus.PREPARATION);
        orderService.updateOrderStatus(order.getId(), OrderStatus.DEPARTED);
        orderService.updateOrderStatus(order.getId(), OrderStatus.ARRIVED);
        orderService.updateOrderStatus(order.getId(), OrderStatus.ARRIVED);

        Inventory savedInventory = inventoryRepository.findById(inventory.getId()).orElseThrow();
        List<StockTransaction> transactions = stockTransactionRepository.findByOrderId(order.getId());

        assertThat(savedInventory.getQuantity()).isEqualTo(15);
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getStatus()).isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    void orderCannotSkipDirectlyFromReceivedToArrived() throws Exception {
        OrderType product = saveProduct("Skip Guard Water");
        Inventory inventory = saveInventory(product, 10);
        orderService.createOrder(orderRequest(product, 5));
        Order order = orderRepository.findAll().get(0);

        assertThatThrownBy(() -> orderService.updateOrderStatus(order.getId(), OrderStatus.ARRIVED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이전 단계");

        Order savedOrder = orderRepository.findById(order.getId()).orElseThrow();
        Inventory savedInventory = inventoryRepository.findById(inventory.getId()).orElseThrow();
        List<StockTransaction> transactions = stockTransactionRepository.findByOrderId(order.getId());

        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.RECEIVED);
        assertThat(savedInventory.getQuantity()).isEqualTo(10);
        assertThat(transactions).isEmpty();
    }

    @Test
    void arrivedOrderCannotMoveBackwardAndIncreaseInventoryAgain() throws Exception {
        OrderType product = saveProduct("Energy Drink");
        Inventory inventory = saveInventory(product, 10);
        orderService.createOrder(orderRequest(product, 5));
        Order order = orderRepository.findAll().get(0);

        orderService.updateOrderStatus(order.getId(), OrderStatus.PREPARATION);
        orderService.updateOrderStatus(order.getId(), OrderStatus.DEPARTED);
        orderService.updateOrderStatus(order.getId(), OrderStatus.ARRIVED);

        assertThatThrownBy(() -> orderService.updateOrderStatus(order.getId(), OrderStatus.RECEIVED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이전 단계");

        orderService.updateOrderStatus(order.getId(), OrderStatus.ARRIVED);

        Inventory savedInventory = inventoryRepository.findById(inventory.getId()).orElseThrow();
        List<StockTransaction> transactions = stockTransactionRepository.findByOrderId(order.getId());
        assertThat(savedInventory.getQuantity()).isEqualTo(15);
        assertThat(transactions).hasSize(1);
    }

    @Test
    void storeOwnerCannotEditOrderAfterHeadquartersStartsProcessing() throws Exception {
        OrderType firstProduct = saveProduct("Triangle Kimbap");
        OrderType secondProduct = saveProduct("Bottled Coffee");
        orderService.createOrder(orderRequest(firstProduct, 4));
        Order order = orderRepository.findAll().get(0);

        orderService.updateOrderStatus(order.getId(), OrderStatus.PREPARATION);

        assertThatThrownBy(() -> orderService.updateOrder(order.getId(), secondProduct.getId(), 8))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("수정할 수 없습니다");
    }

    @Test
    void storeOwnerOnlySeesAndUpdatesOwnOrders() throws Exception {
        User firstOwner = saveUser("firstOwner");
        User secondOwner = saveUser("secondOwner");
        OrderType product = saveProduct("Fresh Sandwich");
        product.setStoreOwner(firstOwner);
        orderTypeRepository.save(product);

        orderService.createOrder(orderRequest(product, 3), firstOwner.getId());
        Order order = orderRepository.findAll().get(0);

        assertThat(orderService.getOrdersByRequester(firstOwner.getId())).hasSize(1);
        assertThat(orderService.getOrdersByRequester(secondOwner.getId())).isEmpty();
        assertThatThrownBy(() -> orderService.getOrderByIdForRequester(order.getId(), secondOwner.getId()))
                .isInstanceOf(OrderNotFoundException.class);
        assertThatThrownBy(() -> orderService.updateOrder(order.getId(), product.getId(), 9, secondOwner.getId()))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void storeCannotRequestOrSwitchToAnotherStoresProduct() {
        User owner = saveUser("stock-owner-one");
        User other = saveUser("stock-owner-two");
        OrderType ownProduct = saveProduct("Own stock item");
        ownProduct.setStoreOwner(owner);
        orderTypeRepository.save(ownProduct);
        OrderType foreignProduct = saveProduct("Other stock item");
        foreignProduct.setStoreOwner(other);
        orderTypeRepository.save(foreignProduct);
        assertThatThrownBy(() -> orderService.createOrder(orderRequest(foreignProduct, 2), owner.getId()))
                .isInstanceOf(OrderNotFoundException.class);
        assertThat(orderRepository.findAll()).isEmpty();
        orderService.createOrder(orderRequest(ownProduct, 2), owner.getId());
        Order order = orderRepository.findAll().get(0);
        assertThatThrownBy(() -> orderService.updateOrder(order.getId(), foreignProduct.getId(), 4, owner.getId()))
                .isInstanceOf(OrderNotFoundException.class);
        assertThat(order.getOrderType().getId()).isEqualTo(ownProduct.getId());
    }

    private OrderType saveProduct(String name) {
        OrderType orderType = new OrderType();
        orderType.setName(name);
        return orderTypeRepository.save(orderType);
    }

    private User saveUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("encoded-password");
        user.setRole(UserRole.STORE_OWNER);
        return userRepository.save(user);
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
