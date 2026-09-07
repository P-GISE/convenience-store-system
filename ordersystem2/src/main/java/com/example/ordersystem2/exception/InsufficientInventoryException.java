package com.example.ordersystem2.exception;

/**
 * 재고가 부족할 때 발생하는 예외 클래스.
 */
public class InsufficientInventoryException extends RuntimeException {
    public InsufficientInventoryException(String message) {
        super(message);
    }
}
