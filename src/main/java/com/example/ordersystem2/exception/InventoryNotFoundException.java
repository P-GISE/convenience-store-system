// src/main/java/com/example/ordersystem2/exception/InventoryNotFoundException.java
package com.example.ordersystem2.exception;

public class InventoryNotFoundException extends RuntimeException {
    public InventoryNotFoundException(String message) {
        super(message);
    }
}
