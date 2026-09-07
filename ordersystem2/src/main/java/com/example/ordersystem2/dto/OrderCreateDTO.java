// src/main/java/com/example/ordersystem2/dto/OrderCreateDTO.java
package com.example.ordersystem2.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class OrderCreateDTO {

    @NotNull(message = "발주 종류는 필수입니다.")
    private Long orderTypeId;

    @NotNull(message = "수량은 필수입니다.")
    @Min(value = 1, message = "수량은 최소 1개 이상이어야 합니다.")
    private Integer quantity;

    // Getters and Setters

    public Long getOrderTypeId() {
        return orderTypeId;
    }

    public void setOrderTypeId(Long orderTypeId) {
        this.orderTypeId = orderTypeId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    // toString() 메소드 (디버깅 용)
    @Override
    public String toString() {
        return "OrderCreateDTO{" +
                "orderTypeId=" + orderTypeId +
                ", quantity=" + quantity +
                '}';
    }
}
