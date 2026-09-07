// src/main/java/com/example/ordersystem2/exception/OrderTypeNotFoundException.java
package com.example.ordersystem2.exception;

public class OrderTypeNotFoundException extends RuntimeException {
  public OrderTypeNotFoundException(String message) {
    super(message);
  }
}
