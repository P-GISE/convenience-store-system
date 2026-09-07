// src/main/java/com/example/ordersystem2/model/OrderStatus.java
package com.example.ordersystem2.model;

public enum OrderStatus {
    RECEIVED("요청 접수"),
    PREPARATION("본사 준비"),
    DEPARTED("출고"),
    ARRIVED("입고 완료");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean canTransitionTo(OrderStatus nextStatus) {
        return nextStatus != null && (nextStatus == this || nextStatus.ordinal() == ordinal() + 1);
    }

    public boolean canStoreOwnerEdit() {
        return this == RECEIVED;
    }

    public boolean isTerminal() {
        return this == ARRIVED;
    }
}
