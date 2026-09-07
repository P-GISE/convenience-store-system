package com.example.ordersystem2.model;

public enum CustomerFulfillmentType {
    PICKUP("매장 픽업"),
    DELIVERY("배달");

    private final String displayName;

    CustomerFulfillmentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
