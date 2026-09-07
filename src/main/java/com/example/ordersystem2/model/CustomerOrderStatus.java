package com.example.ordersystem2.model;

public enum CustomerOrderStatus {
    RECEIVED("주문 접수"),
    PREPARING("준비 중"),
    READY_FOR_PICKUP("픽업 대기"),
    OUT_FOR_DELIVERY("배달 중"),
    COMPLETED("완료"),
    CANCELLED("취소");

    private final String displayName;

    CustomerOrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    public boolean canTransitionTo(CustomerOrderStatus nextStatus, CustomerFulfillmentType fulfillmentType) {
        if (nextStatus == null) {
            return false;
        }
        if (nextStatus == this) {
            return true;
        }
        if (isTerminal()) {
            return false;
        }
        if (nextStatus == CANCELLED) {
            return true;
        }
        if (fulfillmentType == CustomerFulfillmentType.PICKUP) {
            return (this == RECEIVED && nextStatus == PREPARING)
                    || (this == PREPARING && nextStatus == READY_FOR_PICKUP)
                    || (this == READY_FOR_PICKUP && nextStatus == COMPLETED);
        }
        if (fulfillmentType == CustomerFulfillmentType.DELIVERY) {
            return (this == RECEIVED && nextStatus == PREPARING)
                    || (this == PREPARING && nextStatus == OUT_FOR_DELIVERY)
                    || (this == OUT_FOR_DELIVERY && nextStatus == COMPLETED);
        }
        return false;
    }
}
