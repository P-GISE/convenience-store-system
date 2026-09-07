package com.example.ordersystem2.controller;

import com.example.ordersystem2.config.CsrfTokenService;
import com.example.ordersystem2.dto.OrderCreateDTO;
import com.example.ordersystem2.model.Inventory;
import com.example.ordersystem2.model.InventoryStatus;
import com.example.ordersystem2.model.Order;
import com.example.ordersystem2.model.OrderStatus;
import com.example.ordersystem2.model.OrderType;
import com.example.ordersystem2.model.User;
import com.example.ordersystem2.model.UserRole;
import com.example.ordersystem2.repository.InventoryRepository;
import com.example.ordersystem2.repository.OrderRepository;
import com.example.ordersystem2.repository.OrderTypeRepository;
import com.example.ordersystem2.repository.UserRepository;
import com.example.ordersystem2.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.session.store-type=none")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class WebSecurityFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CsrfTokenService csrfTokenService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderTypeRepository orderTypeRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void orderPostWithoutCsrfTokenIsRejected() throws Exception {
        User storeOwner = saveUser("csrfStoreOwner", UserRole.STORE_OWNER);
        OrderType product = saveProduct("CSRF Guard Product");
        MockHttpSession session = sessionFor(storeOwner);

        mockMvc.perform(post("/orders")
                        .session(session)
                        .param("orderTypeId", product.getId().toString())
                        .param("quantity", "3"))
                .andExpect(status().isForbidden());

        assertThat(orderRepository.findAll()).isEmpty();
    }

    @Test
    void orderPostWithSessionCsrfTokenCreatesRequesterScopedOrder() throws Exception {
        User storeOwner = saveUser("csrfAcceptedStoreOwner", UserRole.STORE_OWNER);
        OrderType product = saveProduct("CSRF Accepted Product");
        MockHttpSession session = sessionFor(storeOwner);
        String csrfToken = csrfTokenService.getOrCreateToken(session);

        mockMvc.perform(post("/orders")
                        .session(session)
                        .param("_csrf", csrfToken)
                        .param("orderTypeId", product.getId().toString())
                        .param("quantity", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders?success=request_created"));

        Order order = orderRepository.findAll().get(0);
        assertThat(order.getRequester().getId()).isEqualTo(storeOwner.getId());
    }

    @Test
    void logoutRequiresPostAndCsrfToken() throws Exception {
        User storeOwner = saveUser("logoutStoreOwner", UserRole.STORE_OWNER);
        MockHttpSession session = sessionFor(storeOwner);

        mockMvc.perform(get("/logout").session(session))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(post("/logout").session(session))
                .andExpect(status().isForbidden());

        String csrfToken = csrfTokenService.getOrCreateToken(session);
        mockMvc.perform(post("/logout")
                        .session(session)
                        .param("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout_success"));
    }

    @Test
    void storeOwnerCannotOpenAnotherStoreOwnersEditForm() throws Exception {
        User firstOwner = saveUser("firstMvcOwner", UserRole.STORE_OWNER);
        User secondOwner = saveUser("secondMvcOwner", UserRole.STORE_OWNER);
        OrderType product = saveProduct("Scoped MVC Product");
        orderService.createOrder(orderRequest(product, 4), firstOwner.getId());
        Order order = orderRepository.findAll().get(0);

        mockMvc.perform(get("/orders/edit/{id}", order.getId()).session(sessionFor(secondOwner)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders?error=order_not_found"));
    }

    @Test
    void headquartersCannotSkipFromReceivedToArrivedAtMvcBoundary() throws Exception {
        User headquarters = saveUser("mvcHeadquarters", UserRole.HEADQUARTERS);
        OrderType product = saveProduct("MVC Sequence Product");
        Inventory inventory = saveInventory(product, 10);
        orderService.createOrder(orderRequest(product, 5));
        Order order = orderRepository.findAll().get(0);
        MockHttpSession session = sessionFor(headquarters);
        String csrfToken = csrfTokenService.getOrCreateToken(session);

        mockMvc.perform(post("/dashboard/updateStatus")
                        .session(session)
                        .param("_csrf", csrfToken)
                        .param("orderId", order.getId().toString())
                        .param("status", OrderStatus.ARRIVED.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?error=invalid_status_transition"));

        Order savedOrder = orderRepository.findById(order.getId()).orElseThrow();
        Inventory savedInventory = inventoryRepository.findById(inventory.getId()).orElseThrow();
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.RECEIVED);
        assertThat(savedInventory.getQuantity()).isEqualTo(10);
    }

    private MockHttpSession sessionFor(User user) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("role", user.getRole().name());
        return session;
    }

    private User saveUser(String username, UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("encoded-password");
        user.setRole(role);
        return userRepository.save(user);
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
