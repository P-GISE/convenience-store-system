package com.example.ordersystem2.service;

import com.example.ordersystem2.dto.CustomerOrderCreateDTO;
import com.example.ordersystem2.exception.InsufficientInventoryException;
import com.example.ordersystem2.exception.InventoryNotFoundException;
import com.example.ordersystem2.exception.OrderNotFoundException;
import com.example.ordersystem2.exception.OrderTypeNotFoundException;
import com.example.ordersystem2.model.CustomerFulfillmentType;
import com.example.ordersystem2.model.CustomerOrder;
import com.example.ordersystem2.model.CustomerOrderStatus;
import com.example.ordersystem2.model.Inventory;
import com.example.ordersystem2.model.InventoryStatus;
import com.example.ordersystem2.model.OrderType;
import com.example.ordersystem2.repository.CustomerOrderRepository;
import com.example.ordersystem2.repository.InventoryRepository;
import com.example.ordersystem2.repository.OrderTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerOrderService {

    private final CustomerOrderRepository customerOrderRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderTypeRepository orderTypeRepository;

    public CustomerOrderService(CustomerOrderRepository customerOrderRepository,
                                InventoryRepository inventoryRepository,
                                OrderTypeRepository orderTypeRepository) {
        this.customerOrderRepository = customerOrderRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderTypeRepository = orderTypeRepository;
    }

    public List<Inventory> getAvailableCatalogItems() {
        return inventoryRepository.findByStatus(InventoryStatus.ACTIVE).stream()
                .filter(inventory -> inventory.getQuantity() != null && inventory.getQuantity() > 0)
                .toList();
    }

    public List<CustomerOrder> getAllOrders() {
        return customerOrderRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<CustomerOrder> getOrdersByPhone(String customerPhone) {
        if (customerPhone == null || customerPhone.isBlank()) {
            return List.of();
        }
        return customerOrderRepository.findByCustomerPhoneOrderByCreatedAtDesc(customerPhone.trim());
    }

    @Transactional
    public CustomerOrder createCustomerOrder(CustomerOrderCreateDTO request) {
        OrderType orderType = orderTypeRepository.findById(request.getOrderTypeId())
                .orElseThrow(() -> new OrderTypeNotFoundException("주문 가능한 품목을 찾을 수 없습니다."));

        if (request.getFulfillmentType() == CustomerFulfillmentType.DELIVERY
                && (request.getDeliveryAddress() == null || request.getDeliveryAddress().isBlank())) {
            throw new IllegalArgumentException("배달 주소를 입력해주세요.");
        }

        Inventory inventory = inventoryRepository.findByOrderTypeAndStatusForUpdate(orderType, InventoryStatus.ACTIVE)
                .orElseThrow(() -> new InventoryNotFoundException("현재 판매 가능한 재고가 없습니다."));

        if (inventory.getQuantity() == null || inventory.getQuantity() < request.getQuantity()) {
            throw new InsufficientInventoryException("현재 재고보다 많은 수량은 주문할 수 없습니다.");
        }

        inventory.setQuantity(inventory.getQuantity() - request.getQuantity());
        inventoryRepository.save(inventory);

        CustomerOrder order = new CustomerOrder();
        order.setOrderType(orderType);
        order.setQuantity(request.getQuantity());
        order.setCustomerName(request.getCustomerName().trim());
        order.setCustomerPhone(request.getCustomerPhone().trim());
        order.setFulfillmentType(request.getFulfillmentType());
        order.setDeliveryAddress(trimToNull(request.getDeliveryAddress()));
        order.setRequestMemo(trimToNull(request.getRequestMemo()));
        return customerOrderRepository.save(order);
    }

    @Transactional
    public void updateStatus(Long orderId, CustomerOrderStatus nextStatus) {
        CustomerOrder order = customerOrderRepository.findByIdForStatusUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException("고객 주문을 찾을 수 없습니다."));
        CustomerOrderStatus previousStatus = order.getStatus();
        if (!previousStatus.canTransitionTo(nextStatus, order.getFulfillmentType())) {
            String flowName = order.getFulfillmentType() == CustomerFulfillmentType.PICKUP ? "픽업" : "배달";
            throw new IllegalStateException(flowName + " 주문 상태 흐름에 맞지 않습니다.");
        }

        if (nextStatus == CustomerOrderStatus.CANCELLED && previousStatus != CustomerOrderStatus.CANCELLED) {
            restoreReservedInventory(order);
        }

        order.setStatus(nextStatus);
        customerOrderRepository.save(order);
    }

    private void restoreReservedInventory(CustomerOrder order) {
        Inventory inventory = inventoryRepository.findByOrderTypeAndStatus(order.getOrderType(), InventoryStatus.ACTIVE)
                .orElseThrow(() -> new InventoryNotFoundException("복원할 재고를 찾을 수 없습니다."));
        inventory.setQuantity(inventory.getQuantity() + order.getQuantity());
        inventoryRepository.save(inventory);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
