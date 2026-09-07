package com.example.ordersystem2.service;

import com.example.ordersystem2.dto.CustomerOrderCreateDTO;
import com.example.ordersystem2.exception.InsufficientInventoryException;
import com.example.ordersystem2.model.CustomerFulfillmentType;
import com.example.ordersystem2.model.CustomerOrder;
import com.example.ordersystem2.model.CustomerOrderStatus;
import com.example.ordersystem2.model.Inventory;
import com.example.ordersystem2.model.InventoryStatus;
import com.example.ordersystem2.model.OrderType;
import com.example.ordersystem2.repository.CustomerOrderRepository;
import com.example.ordersystem2.repository.InventoryRepository;
import com.example.ordersystem2.repository.OrderTypeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class CustomerOrderServiceTest {

    @Autowired
    private CustomerOrderService customerOrderService;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private OrderTypeRepository orderTypeRepository;

    @Test
    void pickupCustomerOrderReservesInventoryImmediately() throws Exception {
        OrderType product = saveProduct("도시락");
        Inventory inventory = saveInventory(product, 8);
        CustomerOrderCreateDTO request = customerOrderRequest(product, 3, CustomerFulfillmentType.PICKUP);

        CustomerOrder order = customerOrderService.createCustomerOrder(request);

        Inventory savedInventory = inventoryRepository.findById(inventory.getId()).orElseThrow();
        CustomerOrder savedOrder = customerOrderRepository.findById(order.getId()).orElseThrow();
        assertThat(savedInventory.getQuantity()).isEqualTo(5);
        assertThat(savedOrder.getStatus()).isEqualTo(CustomerOrderStatus.RECEIVED);
        assertThat(savedOrder.getFulfillmentType()).isEqualTo(CustomerFulfillmentType.PICKUP);
        assertThat(savedOrder.getCustomerPhone()).isEqualTo("010-1234-5678");
    }

    @Test
    void deliveryCustomerOrderRequiresAddress() {
        OrderType product = saveProduct("샌드위치");
        saveInventory(product, 5);
        CustomerOrderCreateDTO request = customerOrderRequest(product, 2, CustomerFulfillmentType.DELIVERY);
        request.setDeliveryAddress(" ");

        assertThatThrownBy(() -> customerOrderService.createCustomerOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("배달 주소");
    }

    @Test
    void customerOrderCannotExceedActiveInventory() {
        OrderType product = saveProduct("우유");
        Inventory inventory = saveInventory(product, 2);
        CustomerOrderCreateDTO request = customerOrderRequest(product, 3, CustomerFulfillmentType.PICKUP);

        assertThatThrownBy(() -> customerOrderService.createCustomerOrder(request))
                .isInstanceOf(InsufficientInventoryException.class);

        Inventory savedInventory = inventoryRepository.findById(inventory.getId()).orElseThrow();
        assertThat(savedInventory.getQuantity()).isEqualTo(2);
        assertThat(customerOrderRepository.findAll()).isEmpty();
    }

    @Test
    void pickupOrderCannotMoveToDeliveryOnlyStatus() throws Exception {
        OrderType product = saveProduct("아이스커피");
        saveInventory(product, 6);
        CustomerOrder order = customerOrderService.createCustomerOrder(
                customerOrderRequest(product, 1, CustomerFulfillmentType.PICKUP));

        assertThatThrownBy(() -> customerOrderService.updateStatus(order.getId(), CustomerOrderStatus.OUT_FOR_DELIVERY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("픽업");

        CustomerOrder savedOrder = customerOrderRepository.findById(order.getId()).orElseThrow();
        assertThat(savedOrder.getStatus()).isEqualTo(CustomerOrderStatus.RECEIVED);
    }

    @Test
    void cancelledCustomerOrderRestoresReservedInventory() throws Exception {
        OrderType product = saveProduct("핫바");
        Inventory inventory = saveInventory(product, 4);
        CustomerOrder order = customerOrderService.createCustomerOrder(
                customerOrderRequest(product, 2, CustomerFulfillmentType.PICKUP));

        customerOrderService.updateStatus(order.getId(), CustomerOrderStatus.CANCELLED);

        Inventory savedInventory = inventoryRepository.findById(inventory.getId()).orElseThrow();
        CustomerOrder savedOrder = customerOrderRepository.findById(order.getId()).orElseThrow();
        assertThat(savedInventory.getQuantity()).isEqualTo(4);
        assertThat(savedOrder.getStatus()).isEqualTo(CustomerOrderStatus.CANCELLED);
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

    private CustomerOrderCreateDTO customerOrderRequest(OrderType product, int quantity, CustomerFulfillmentType type) {
        CustomerOrderCreateDTO request = new CustomerOrderCreateDTO();
        request.setOrderTypeId(product.getId());
        request.setQuantity(quantity);
        request.setCustomerName("홍길동");
        request.setCustomerPhone("010-1234-5678");
        request.setFulfillmentType(type);
        request.setDeliveryAddress("서울시 강남구 테스트로 1");
        request.setRequestMemo("문 앞에 두지 마세요");
        return request;
    }
}
