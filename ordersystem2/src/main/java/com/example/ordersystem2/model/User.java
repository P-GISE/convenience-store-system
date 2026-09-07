// src/main/java/com/example/ordersystem2/model/User.java
package com.example.ordersystem2.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users") // 테이블 이름을 'users'로 설정
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true) // 사용자명은 고유해야 함
    private String username;

    @Column(nullable = false)
    private String password;

    // 기본 생성자
    public User() {}

    // 생성자
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    // ID는 Setter가 필요 없을 수 있으므로 생략 가능

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
