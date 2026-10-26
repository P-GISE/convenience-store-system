package com.example.ordersystem2.controller;

import com.example.ordersystem2.config.CsrfTokenService;
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
import com.example.ordersystem2.repository.OrderTypeRepository;
import com.example.ordersystem2.repository.UserRepository;
import com.example.ordersystem2.service.CustomerOrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.session.store-type=none")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class CustomerOrderWebFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CsrfTokenService csrfTokenService;

    @Autowired
    private CustomerOrderService customerOrderService;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private OrderTypeRepository orderTypeRepository;

    @Autowired
    private UserRepository userRepository;

    private User customer;
    private User owner;

    @BeforeEach
    void users() {
        customer = saveUser("synthetic-web-customer", UserRole.CUSTOMER);
        owner = saveUser("synthetic-web-owner", UserRole.STORE_OWNER);
    }

    @Test
    void publicShopShowsActiveInventoryAndFulfillmentChoices() throws Exception {
        OrderType product = saveProduct("즉석커피");
        saveInventory(product, 9);

        mockMvc.perform(get("/shop"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("즉석커피")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("로그인하고 주문하기")));
    }

    @Test
    void publicCustomerOrderRequiresCsrfAndReservesInventory() throws Exception {
        OrderType product = saveProduct("초코바");
        Inventory inventory = saveInventory(product, 6);
        MockHttpSession session = sessionFor(customer);

        mockMvc.perform(post("/shop")
                        .session(session)
                        .param("orderTypeId", product.getId().toString())
                        .param("quantity", "2")
                        .param("customerName", "김고객")
                        .param("customerPhone", "010-9999-1111")
                        .param("fulfillmentType", CustomerFulfillmentType.PICKUP.name()))
                .andExpect(status().isForbidden());

        String csrfToken = csrfTokenService.getOrCreateToken(session);
        mockMvc.perform(post("/shop")
                        .session(session)
                        .param("_csrf", csrfToken)
                        .param("orderTypeId", product.getId().toString())
                        .param("quantity", "2")
                        .param("customerName", "김고객")
                        .param("customerPhone", "010-9999-1111")
                        .param("fulfillmentType", CustomerFulfillmentType.PICKUP.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shop?success=created"));

        Inventory savedInventory = inventoryRepository.findById(inventory.getId()).orElseThrow();
        CustomerOrder order = customerOrderRepository.findAll().get(0);
        assertThat(savedInventory.getQuantity()).isEqualTo(4);
        assertThat(order.getCustomerName()).isEqualTo("김고객");
    }

    @Test
    void storeOwnerCanViewAndAdvanceCustomerOrders() throws Exception {
        User storeOwner = owner;
        OrderType product = saveProduct("치킨샐러드");
        saveInventory(product, 5);
        CustomerOrder order = customerOrderService.createCustomerOrder(customerOrderRequest(product, 1), customer.getId());
        MockHttpSession session = sessionFor(storeOwner);

        mockMvc.perform(get("/customer-orders").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("치킨샐러드")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("주문 접수")));

        String csrfToken = csrfTokenService.getOrCreateToken(session);
        mockMvc.perform(post("/customer-orders/{id}/status", order.getId())
                        .session(session)
                        .param("_csrf", csrfToken)
                        .param("status", CustomerOrderStatus.PREPARING.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customer-orders?success=status_updated"));

        CustomerOrder savedOrder = customerOrderRepository.findById(order.getId()).orElseThrow();
        assertThat(savedOrder.getStatus()).isEqualTo(CustomerOrderStatus.PREPARING);
    }

    @Test
    void anonymousUsersCannotOpenCustomerOrderOperations() throws Exception {
        mockMvc.perform(get("/customer-orders"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void headquartersCannotOperateStoreCustomerOrders() throws Exception {
        User headquarters = saveUser("customerOpsHeadquarters", UserRole.HEADQUARTERS);
        OrderType product = saveProduct("본사 제한 상품");
        saveInventory(product, 5);
        CustomerOrder order = customerOrderService.createCustomerOrder(customerOrderRequest(product, 1), customer.getId());
        MockHttpSession session = sessionFor(headquarters);

        mockMvc.perform(get("/customer-orders").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home2"));

        String csrfToken = csrfTokenService.getOrCreateToken(session);
        mockMvc.perform(post("/customer-orders/{id}/status", order.getId())
                        .session(session)
                        .param("_csrf", csrfToken)
                        .param("status", CustomerOrderStatus.PREPARING.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home2"));

        CustomerOrder savedOrder = customerOrderRepository.findById(order.getId()).orElseThrow();
        assertThat(savedOrder.getStatus()).isEqualTo(CustomerOrderStatus.RECEIVED);
    }

    @Test
    void publicSignupCannotChoosePrivilegedRoleAndCustomerLoginRotatesSession() throws Exception {
        MockHttpSession signupSession = new MockHttpSession();
        String token = csrfTokenService.getOrCreateToken(signupSession);
        mockMvc.perform(post("/signup").session(signupSession).param("_csrf", token)
                        .param("username", "public-customer").param("password", "synthetic-password-123")
                        .param("confirmPassword", "synthetic-password-123").param("role", "HEADQUARTERS"))
                .andExpect(redirectedUrl("/login?signup_success"));
        User registered = userRepository.findByUsername("public-customer");
        assertThat(registered.getRole()).isEqualTo(UserRole.CUSTOMER);
        var login = mockMvc.perform(post("/login").session(signupSession).param("_csrf", token)
                        .param("username", "public-customer").param("password", "synthetic-password-123"))
                .andExpect(redirectedUrl("/shop")).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(signupSession.isInvalid()).isTrue();
        assertThat(session.getAttribute("userId")).isEqualTo(registered.getId());
        for (String route : new String[]{"/home2", "/inventory", "/orders", "/dashboard", "/stock-transactions", "/customer-orders"}) {
            mockMvc.perform(get(route).session(session)).andExpect(redirectedUrl("/shop"));
        }
    }

    @Test
    void customersOnlySeeOwnOrdersAndAnonymousPhoneLookupRevealsNothing() throws Exception {
        User second = saveUser("second-customer", UserRole.CUSTOMER);
        OrderType product = saveProduct("scope-product");
        saveInventory(product, 5);
        CustomerOrder firstOrder = customerOrderService.createCustomerOrder(customerOrderRequest(product, 1), customer.getId());
        CustomerOrder secondOrder = customerOrderService.createCustomerOrder(customerOrderRequest(product, 1), second.getId());
        var firstView = mockMvc.perform(get("/shop").session(sessionFor(customer)).param("phone", secondOrder.getCustomerPhone()))
                .andExpect(status().isOk()).andReturn();
        assertThat((java.util.List<?>) firstView.getModelAndView().getModel().get("lookupOrders"))
                .isEqualTo(java.util.List.of(firstOrder));
        var secondView = mockMvc.perform(get("/shop").session(sessionFor(second)))
                .andExpect(status().isOk()).andReturn();
        assertThat((java.util.List<?>) secondView.getModelAndView().getModel().get("lookupOrders"))
                .isEqualTo(java.util.List.of(secondOrder));
        var anonymous = mockMvc.perform(get("/shop").param("phone", firstOrder.getCustomerPhone()))
                .andExpect(status().isOk()).andReturn();
        assertThat((java.util.List<?>) anonymous.getModelAndView().getModel().get("lookupOrders")).isEmpty();
    }

    @Test
    void storeCannotViewOrChangeOtherStoresOrdersEvenWithForgedSessionRole() throws Exception {
        User secondOwner = saveUser("second-owner", UserRole.STORE_OWNER);
        OrderType product = saveProduct("first-store-product");
        OrderType secondProduct = saveProduct("second-store-product");
        secondProduct.setStoreOwner(secondOwner);
        orderTypeRepository.save(secondProduct);
        saveInventory(product, 5);
        saveInventory(secondProduct, 5);
        CustomerOrder first = customerOrderService.createCustomerOrder(customerOrderRequest(product, 1), customer.getId());
        CustomerOrder second = customerOrderService.createCustomerOrder(customerOrderRequest(secondProduct, 1), customer.getId());
        MockHttpSession session = sessionFor(owner);
        session.setAttribute("role", "HEADQUARTERS");
        var view = mockMvc.perform(get("/customer-orders").session(session)).andExpect(status().isOk()).andReturn();
        assertThat((java.util.List<?>) view.getModelAndView().getModel().get("customerOrders")).isEqualTo(java.util.List.of(first));
        mockMvc.perform(post("/customer-orders/{id}/status", second.getId()).session(session)
                        .param("_csrf", csrfTokenService.getOrCreateToken(session)).param("status", "CANCELLED"))
                .andExpect(status().isNotFound());
        assertThat(second.getStatus()).isEqualTo(CustomerOrderStatus.RECEIVED);
        assertThat(inventoryRepository.findByOrderType(secondProduct).orElseThrow().getQuantity()).isEqualTo(4);
    }

    @Test
    void ownershipParametersCannotAssignAnotherCustomerOrStore() throws Exception {
        User otherCustomer = saveUser("forged-customer", UserRole.CUSTOMER);
        User otherOwner = saveUser("forged-owner", UserRole.STORE_OWNER);
        OrderType product = saveProduct("ownership-product");
        saveInventory(product, 5);
        MockHttpSession session = sessionFor(customer);
        mockMvc.perform(post("/shop").session(session)
                        .param("_csrf", csrfTokenService.getOrCreateToken(session))
                        .param("orderTypeId", product.getId().toString()).param("quantity", "1")
                        .param("customerName", "Synthetic customer").param("customerPhone", "synthetic-phone")
                        .param("fulfillmentType", "PICKUP")
                        .param("customerId", otherCustomer.getId().toString()).param("storeOwnerId", otherOwner.getId().toString()))
                .andExpect(redirectedUrl("/shop?success=created"));
        CustomerOrder saved = customerOrderRepository.findAll().get(0);
        assertThat(saved.getCustomer().getId()).isEqualTo(customer.getId());
        assertThat(saved.getStoreOwner().getId()).isEqualTo(owner.getId());
    }

    @Test
    void anonymousOrderAndMultipleStoreProductsAreRejectedWithoutReservingInventory() throws Exception {
        OrderType first = saveProduct("mixed-store-first");
        OrderType second = saveProduct("mixed-store-second");
        second.setStoreOwner(saveUser("mixed-second-owner", UserRole.STORE_OWNER));
        orderTypeRepository.save(second);
        Inventory stock = saveInventory(first, 5);
        saveInventory(second, 5);
        MockHttpSession anonymous = new MockHttpSession();
        mockMvc.perform(post("/shop").session(anonymous).param("_csrf", csrfTokenService.getOrCreateToken(anonymous))
                        .param("orderTypeId", first.getId().toString()).param("quantity", "1")
                        .param("customerName", "Synthetic").param("customerPhone", "synthetic-phone").param("fulfillmentType", "PICKUP"))
                .andExpect(redirectedUrl("/login"));
        MockHttpSession session = sessionFor(customer);
        mockMvc.perform(post("/shop").session(session).param("_csrf", csrfTokenService.getOrCreateToken(session))
                        .param("orderTypeId", first.getId().toString(), second.getId().toString()).param("quantity", "1")
                        .param("customerName", "Synthetic").param("customerPhone", "synthetic-phone").param("fulfillmentType", "PICKUP"))
                .andExpect(status().isBadRequest());
        assertThat(customerOrderRepository.findAll()).isEmpty();
        assertThat(inventoryRepository.findById(stock.getId()).orElseThrow().getQuantity()).isEqualTo(5);
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
        orderType.setStoreOwner(owner);
        return orderTypeRepository.save(orderType);
    }

    private Inventory saveInventory(OrderType product, int quantity) {
        Inventory inventory = new Inventory();
        inventory.setOrderType(product);
        inventory.setQuantity(quantity);
        inventory.setStatus(InventoryStatus.ACTIVE);
        return inventoryRepository.save(inventory);
    }

    private CustomerOrderCreateDTO customerOrderRequest(OrderType product, int quantity) {
        CustomerOrderCreateDTO request = new CustomerOrderCreateDTO();
        request.setOrderTypeId(product.getId());
        request.setQuantity(quantity);
        request.setCustomerName("박고객");
        request.setCustomerPhone("010-2222-3333");
        request.setFulfillmentType(CustomerFulfillmentType.PICKUP);
        request.setDeliveryAddress(Objects.toString(null, ""));
        return request;
    }
}
