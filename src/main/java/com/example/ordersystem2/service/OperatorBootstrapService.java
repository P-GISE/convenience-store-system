package com.example.ordersystem2.service;

import com.example.ordersystem2.model.Inventory;
import com.example.ordersystem2.model.InventoryStatus;
import com.example.ordersystem2.model.OrderType;
import com.example.ordersystem2.model.User;
import com.example.ordersystem2.model.UserRole;
import com.example.ordersystem2.repository.InventoryRepository;
import com.example.ordersystem2.repository.OrderTypeRepository;
import com.example.ordersystem2.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

@Service
public class OperatorBootstrapService {
    private final UserRepository users;
    private final UserService userService;
    private final OrderTypeRepository products;
    private final InventoryRepository inventory;

    public OperatorBootstrapService(UserRepository users, UserService userService,
                                    OrderTypeRepository products, InventoryRepository inventory) {
        this.users = users;
        this.userService = userService;
        this.products = products;
        this.inventory = inventory;
    }

    @Transactional
    public void apply(JsonNode input) {
        String operation = text(input, "operation");
        if (operation.equals("create-user")) {
            fields(input, Set.of("operation", "username", "password", "role"));
            String username = text(input, "username");
            String password = text(input, "password");
            UserRole role = UserRole.valueOf(text(input, "role"));
            if (!username.matches("[A-Za-z0-9_-]{3,64}") || password.length() < 16
                    || password.getBytes(StandardCharsets.UTF_8).length > 72
                    || (role != UserRole.STORE_OWNER && role != UserRole.HEADQUARTERS)
                    || users.findByUsername(username) != null) {
                throw new IllegalArgumentException("invalid_or_existing_operator_account");
            }
            userService.registerUser(username, password, role);
        } else if (operation.equals("create-product")) {
            fields(input, Set.of("operation", "name", "storeOwnerUsername", "quantity"));
            String name = text(input, "name").trim();
            User owner = users.findByUsername(text(input, "storeOwnerUsername"));
            JsonNode quantity = input.get("quantity");
            if (name.isEmpty() || name.length() > 255 || owner == null || owner.getRole() != UserRole.STORE_OWNER
                    || quantity == null || !quantity.isIntegralNumber() || !quantity.canConvertToInt()
                    || quantity.intValue() < 0) {
                throw new IllegalArgumentException("invalid_product_or_store");
            }
            OrderType product = new OrderType();
            product.setName(name);
            product.setStoreOwner(owner);
            products.save(product);
            Inventory stock = new Inventory();
            stock.setOrderType(product);
            stock.setQuantity(quantity.intValue());
            stock.setStatus(InventoryStatus.ACTIVE);
            inventory.save(stock);
        } else {
            throw new IllegalArgumentException("unsupported_operation");
        }
    }

    private static String text(JsonNode input, String key) {
        if (input == null || !input.isObject() || !input.hasNonNull(key) || !input.get(key).isTextual()) {
            throw new IllegalArgumentException("invalid_input");
        }
        return input.get(key).textValue();
    }

    private static void fields(JsonNode input, Set<String> expected) {
        Set<String> actual = new HashSet<>();
        input.fieldNames().forEachRemaining(actual::add);
        if (!actual.equals(expected)) {
            throw new IllegalArgumentException("unexpected_input_fields");
        }
    }
}
