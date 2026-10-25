package com.example.ordersystem2.service;

import com.example.ordersystem2.model.User;
import com.example.ordersystem2.model.UserRole;
import com.example.ordersystem2.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void registerUserStoresRequestedRole() {
        User user = userService.registerUser("hquser", "password123", UserRole.HEADQUARTERS);

        User saved = userRepository.findByUsername("hquser");

        assertThat(saved.getId()).isEqualTo(user.getId());
        assertThat(saved.getRole()).isEqualTo(UserRole.HEADQUARTERS);
    }

    @Test
    void registerUserDefaultsToCustomerWhenRoleIsMissing() {
        userService.registerUser("storeuser", "password123", null);

        User saved = userRepository.findByUsername("storeuser");

        assertThat(saved.getRole()).isEqualTo(UserRole.CUSTOMER);
    }
}
