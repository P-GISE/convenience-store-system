package com.example.ordersystem2.config;

import com.example.ordersystem2.model.Inventory;
import com.example.ordersystem2.model.InventoryStatus;
import com.example.ordersystem2.model.OrderType;
import com.example.ordersystem2.repository.InventoryRepository;
import com.example.ordersystem2.repository.OrderTypeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("demo")
@Order(2)
public class DemoCatalogInitializer implements CommandLineRunner {

    private final OrderTypeRepository orderTypeRepository;
    private final InventoryRepository inventoryRepository;

    public DemoCatalogInitializer(OrderTypeRepository orderTypeRepository,
                                  InventoryRepository inventoryRepository) {
        this.orderTypeRepository = orderTypeRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public void run(String... args) {
        List<OrderType> products = orderTypeRepository.findAll();
        for (OrderType product : products) {
            inventoryRepository.findByOrderType(product).orElseGet(() -> saveInventory(product));
        }
    }

    private Inventory saveInventory(OrderType product) {
        Inventory inventory = new Inventory();
        inventory.setOrderType(product);
        inventory.setQuantity(12);
        inventory.setStatus(InventoryStatus.ACTIVE);
        return inventoryRepository.save(inventory);
    }
}
