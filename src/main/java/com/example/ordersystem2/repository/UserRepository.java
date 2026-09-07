// src/main/java/com/example/ordersystem2/repository/UserRepository.java
package com.example.ordersystem2.repository;

import com.example.ordersystem2.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // 사용자명으로 사용자 찾기
    User findByUsername(String username);
}
