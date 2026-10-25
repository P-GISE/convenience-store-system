package com.example.ordersystem2.service;

import com.example.ordersystem2.model.UserRole;
import com.example.ordersystem2.repository.InventoryRepository;
import com.example.ordersystem2.repository.OrderTypeRepository;
import com.example.ordersystem2.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class OperatorBootstrapServiceTest {
    @Autowired OperatorBootstrapService bootstrap;
    @Autowired UserRepository users;
    @Autowired OrderTypeRepository products;
    @Autowired InventoryRepository inventory;
    @Autowired UserService userService;
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void explicitOperatorCreatesHashedAccountAndAssignedProduct() throws Exception {
        bootstrap.apply(mapper.readTree("""
                {"operation":"create-user","username":"operator-store","password":"synthetic-only-123456","role":"STORE_OWNER"}
                """));
        var owner = users.findByUsername("operator-store");
        assertThat(owner.getRole()).isEqualTo(UserRole.STORE_OWNER);
        assertThat(owner.getPassword()).isNotEqualTo("synthetic-only-123456");
        assertThat(userService.authenticate("operator-store", "synthetic-only-123456")).isNotNull();
        bootstrap.apply(mapper.readTree("""
                {"operation":"create-product","name":"Synthetic item","storeOwnerUsername":"operator-store","quantity":7}
                """));
        var stock = inventory.findAll().get(0);
        assertThat(stock.getOrderType().getStoreOwner().getId()).isEqualTo(owner.getId());
        assertThat(stock.getQuantity()).isEqualTo(7);
    }

    @Test
    void bootstrapDoesNotOverwriteAccountOrAcceptCustomerAsStore() throws Exception {
        var original = userService.registerUser("existing-customer", "synthetic-only-123456");
        assertThatThrownBy(() -> bootstrap.apply(mapper.readTree("""
                {"operation":"create-user","username":"existing-customer","password":"replacement-password-123","role":"HEADQUARTERS"}
                """))).isInstanceOf(IllegalArgumentException.class);
        assertThat(users.findByUsername("existing-customer").getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThatThrownBy(() -> bootstrap.apply(mapper.readTree("""
                {"operation":"create-product","name":"Synthetic item","storeOwnerUsername":"existing-customer","quantity":7}
                """))).isInstanceOf(IllegalArgumentException.class);
        assertThat(inventory.findAll()).isEmpty();
        assertThat(userService.authenticate(original.getUsername(), "synthetic-only-123456")).isNotNull();
    }

    @Test
    void invalidInputCannotCreatePrivilegeOrStock() throws Exception {
        long before = products.count();
        for (String payload : new String[]{
                "{\"operation\":\"create-user\",\"username\":\"bad\",\"password\":\"short\",\"role\":\"HEADQUARTERS\"}",
                "{\"operation\":\"create-user\",\"username\":\"bad\",\"password\":\"synthetic-long-password\",\"role\":\"CUSTOMER\"}",
                "{\"operation\":\"create-user\",\"username\":\"bad\",\"password\":\"synthetic-long-password\",\"role\":\"STORE_OWNER\",\"id\":1}",
                "{\"operation\":\"create-product\",\"name\":\"bad\",\"storeOwnerUsername\":\"missing\",\"quantity\":-1}"}) {
            assertThatThrownBy(() -> bootstrap.apply(mapper.readTree(payload)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(users.findByUsername("bad")).isNull();
        assertThat(products.count()).isEqualTo(before);
    }
}
