package com.example.ordersystem2.exception;

/**
 * 주문을 찾을 수 없을 때 발생하는 예외 클래스.
 */
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String message) {
        super(message);
    }
}
