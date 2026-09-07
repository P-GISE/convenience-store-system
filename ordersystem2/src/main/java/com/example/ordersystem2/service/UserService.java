// src/main/java/com/example/ordersystem2/service/UserService.java
package com.example.ordersystem2.service;

import com.example.ordersystem2.model.User;
import com.example.ordersystem2.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.mindrot.jbcrypt.BCrypt; // jBCrypt 사용
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    // 사용자 인증 메소드
    public User authenticate(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user != null) {
            try {
                if (BCrypt.checkpw(password, user.getPassword())) {
                    return user;
                }
            } catch (IllegalArgumentException e) {
                // 비밀번호 해시가 잘못된 경우
                logger.error("Invalid password hash for user: {}", username, e);
                // 추가 조치 가능 (예: 관리자에게 알림, 사용자에게 비밀번호 재설정 요구 등)
            }
        }
        return null;
    }

    // 사용자 ID로 사용자 찾기
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    // 사용자 등록 메소드
    public User registerUser(String username, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt())); // 비밀번호 암호화
        return userRepository.save(user);
    }

    // 사용자명으로 사용자 찾기
    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
