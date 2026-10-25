package com.example.ordersystem2.config;

import com.example.ordersystem2.model.User;
import com.example.ordersystem2.model.UserRole;
import com.example.ordersystem2.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

@Component
@Profile("demo")
@Order(0)
public class DemoUserInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    public DemoUserInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        createUserIfMissing("store_demo", "pw123456", UserRole.STORE_OWNER);
        createUserIfMissing("hq_demo", "pw123456", UserRole.HEADQUARTERS);
        createUserIfMissing("customer_demo", "pw123456", UserRole.CUSTOMER);
    }

    private void createUserIfMissing(String username, String password, UserRole role) {
        if (userRepository.findByUsername(username) != null) {
            return;
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setRole(role);
        userRepository.save(user);
    }
}
