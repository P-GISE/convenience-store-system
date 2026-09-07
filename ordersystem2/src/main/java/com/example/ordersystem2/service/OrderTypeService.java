// src/main/java/com/example/ordersystem2/service/OrderTypeService.java
package com.example.ordersystem2.service;

import com.example.ordersystem2.exception.OrderTypeNotFoundException;
import com.example.ordersystem2.model.OrderType;
import com.example.ordersystem2.repository.OrderTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderTypeService {

    @Autowired
    private OrderTypeRepository orderTypeRepository;

    /**
     * 모든 발주 종류를 조회합니다.
     * @return 발주 종류 목록
     */
    public List<OrderType> getAllOrderTypes() {
        return orderTypeRepository.findAll();
    }

    /**
     * 특정 발주 종류를 ID로 조회합니다.
     * @param id 발주 종류 ID
     * @return OrderType 객체
     * @throws OrderTypeNotFoundException 발주 종류를 찾을 수 없는 경우
     */
    public OrderType getOrderTypeById(Long id) throws OrderTypeNotFoundException {
        return orderTypeRepository.findById(id)
                .orElseThrow(() -> new OrderTypeNotFoundException("발주 종류를 찾을 수 없습니다. ID: " + id));
    }
}
