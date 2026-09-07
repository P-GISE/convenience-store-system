package com.example.ordersystem2.dto;

public class UserResponseDTO {

    private Long id;
    private String username;

    // Constructors
    public UserResponseDTO() {}

    public UserResponseDTO(Long id, String username) {
        this.id = id;
        this.username = username;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}