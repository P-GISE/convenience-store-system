package com.example.ordersystem2.config;

import com.example.ordersystem2.model.OrderType;
import com.example.ordersystem2.repository.OrderTypeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class DataInitializer {

    @Bean
    @Order(1)
    CommandLineRunner seedProducts(OrderTypeRepository orderTypeRepository) {
        return args -> {
            if (orderTypeRepository.count() > 0) {
                return;
            }

            saveProduct(orderTypeRepository, "삼각김밥");
            saveProduct(orderTypeRepository, "생수");
            saveProduct(orderTypeRepository, "컵라면");
            saveProduct(orderTypeRepository, "에너지드링크");
        };
    }

    private void saveProduct(OrderTypeRepository orderTypeRepository, String name) {
        OrderType orderType = new OrderType();
        orderType.setName(name);
        orderTypeRepository.save(orderType);
    }
}
