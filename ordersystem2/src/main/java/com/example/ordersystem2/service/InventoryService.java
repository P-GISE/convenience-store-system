// src/main/java/com/example/ordersystem2/service/InventoryService.java
package com.example.ordersystem2.service;

import com.example.ordersystem2.dto.InventoryCreateDTO;
import com.example.ordersystem2.dto.InventoryEditDTO;
import com.example.ordersystem2.exception.InventoryNotFoundException;
import com.example.ordersystem2.model.Inventory;
import com.example.ordersystem2.model.OrderType;
import com.example.ordersystem2.repository.InventoryRepository;
import com.example.ordersystem2.repository.OrderTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class InventoryService {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private OrderTypeRepository orderTypeRepository;

    @Autowired
    private OrderTypeService orderTypeService;

    /**
     * 재고를 수정합니다.
     * @param id 재고 ID
     * @param inventoryEditDTO 수정된 재고 DTO
     * @throws Exception 재고를 찾을 수 없거나 중복된 OrderType인 경우
     */
    @Transactional
    public void updateInventory(Long id, InventoryEditDTO inventoryEditDTO) throws Exception {
        // 재고 조회
        Inventory inventory = getInventoryById(id);

        // 발주 종류 조회
        OrderType orderType = orderTypeRepository.findById(inventoryEditDTO.getOrderTypeId())
                .orElseThrow(() -> new Exception("발주 종류를 찾을 수 없습니다. ID: " + inventoryEditDTO.getOrderTypeId()));

        // 동일한 OrderType으로 중복되면 안 되므로, 다른 Inventory에 동일 OrderType이 있는지 확인
        Optional<Inventory> existingInventoryOpt = inventoryRepository.findByOrderType(orderType);
        if (existingInventoryOpt.isPresent() && !existingInventoryOpt.get().getId().equals(id)) {
            throw new Exception("이미 동일한 발주 종류로 등록된 재고가 존재합니다.");
        }

        // 재고 업데이트
        inventory.setOrderType(orderType);
        inventory.setQuantity(inventoryEditDTO.getQuantity());
        inventoryRepository.save(inventory);
    }

    /**
     * 특정 재고를 ID로 조회합니다.
     * @param id 재고 ID
     * @return Inventory 객체
     * @throws InventoryNotFoundException 재고를 찾을 수 없는 경우
     */
    public Inventory getInventoryById(Long id) throws InventoryNotFoundException {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new InventoryNotFoundException("재고를 찾을 수 없습니다. ID: " + id));
    }

    /**
     * 특정 발주 종류에 해당하는 재고를 조회합니다.
     * @param orderType 발주 종류
     * @return Inventory 객체
     * @throws InventoryNotFoundException 재고를 찾을 수 없는 경우
     */
    public Inventory getInventoryByOrderType(OrderType orderType) throws InventoryNotFoundException {
        return inventoryRepository.findByOrderType(orderType)
                .orElseThrow(() -> new InventoryNotFoundException("발주 종류에 해당하는 재고를 찾을 수 없습니다. 발주 종류 ID: " + orderType.getId()));
    }

    /**
     * 모든 재고를 조회합니다.
     * @return 재고 목록
     */
    public List<Inventory> getAllInventories() {
        return inventoryRepository.findAll();
    }

    /**
     * 재고를 저장합니다.
     * @param inventory 재고 객체
     */
    public void saveInventory(Inventory inventory) {
        inventoryRepository.save(inventory);
    }

    /**
     * 재고를 추가합니다.
     * @param inventoryCreateDTO 재고 생성 DTO
     */
    public void addInventory(InventoryCreateDTO inventoryCreateDTO) {
        OrderType orderType = orderTypeService.getOrderTypeById(inventoryCreateDTO.getOrderTypeId());
        Inventory inventory = new Inventory();
        inventory.setOrderType(orderType);
        inventory.setQuantity(inventoryCreateDTO.getQuantity());
        inventoryRepository.save(inventory);
    }

    /**
     * 재고를 수정합니다.
     * @param id 재고 ID
     * @param updatedInventory 수정된 재고 객체
     * @throws InventoryNotFoundException 재고를 찾을 수 없는 경우
     */
    public void updateInventory(Long id, Inventory updatedInventory) throws InventoryNotFoundException {
        Inventory existingInventory = getInventoryById(id);
        existingInventory.setOrderType(updatedInventory.getOrderType());
        existingInventory.setQuantity(updatedInventory.getQuantity());
        inventoryRepository.save(existingInventory);
    }

    /**
     * 재고를 삭제합니다.
     * @param id 재고 ID
     * @throws InventoryNotFoundException 재고를 찾을 수 없는 경우
     */
    public void deleteInventory(Long id) throws InventoryNotFoundException {
        Inventory existingInventory = getInventoryById(id);
        inventoryRepository.delete(existingInventory);
    }
}
