package com.example.ordersystem2.service;

import com.example.ordersystem2.dto.InventoryCreateDTO;
import com.example.ordersystem2.model.Inventory;
import com.example.ordersystem2.model.InventoryStatus;
import com.example.ordersystem2.model.OrderType;
import com.example.ordersystem2.repository.InventoryRepository;
import com.example.ordersystem2.repository.OrderTypeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class InventoryServiceTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private OrderTypeRepository orderTypeRepository;

    @Test
    void addOrUpdateInventoryUpdatesExistingActiveInventory() {
        OrderType product = saveProduct("Energy Drink");
        Inventory existing = saveInventory(product, 10);
        InventoryCreateDTO request = inventoryRequest(product, 25);

        inventoryService.addOrUpdateInventory(request);
        inventoryRepository.flush();

        Inventory saved = inventoryRepository.findById(existing.getId()).orElseThrow();
        assertThat(saved.getQuantity()).isEqualTo(25);
        assertThat(inventoryRepository.findAll()).hasSize(1);
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

    private InventoryCreateDTO inventoryRequest(OrderType product, int quantity) {
        InventoryCreateDTO request = new InventoryCreateDTO();
        request.setOrderTypeId(product.getId());
        request.setQuantity(quantity);
        return request;
    }
}
