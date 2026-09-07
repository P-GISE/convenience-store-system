// src/main/java/com/example/ordersystem2/service/InventoryService.java
package com.example.ordersystem2.service;

import com.example.ordersystem2.dto.InventoryCreateDTO;
import com.example.ordersystem2.dto.InventoryEditDTO;
import com.example.ordersystem2.exception.InventoryNotFoundException;
import com.example.ordersystem2.model.Inventory;
import com.example.ordersystem2.model.InventoryStatus;
import com.example.ordersystem2.model.OrderType;
import com.example.ordersystem2.model.StockTransaction;
import com.example.ordersystem2.model.TransactionStatus;
import com.example.ordersystem2.repository.InventoryRepository;
import com.example.ordersystem2.repository.OrderTypeRepository;
import com.example.ordersystem2.repository.StockTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private OrderTypeRepository orderTypeRepository;

    @Autowired
    private OrderTypeService orderTypeService;

    @Autowired
    private StockTransactionRepository stockTransactionRepository;

    @Autowired
    private StockTransactionService stockTransactionService;

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

    @Transactional
    public Inventory getOrCreateInventoryByOrderType(OrderType orderType) {
        return inventoryRepository.findByOrderType(orderType)
                .orElseGet(() -> {
                    Inventory inventory = new Inventory();
                    inventory.setOrderType(orderType);
                    inventory.setQuantity(0);
                    inventory.setStatus(InventoryStatus.ACTIVE);
                    Inventory savedInventory = inventoryRepository.save(inventory);
                    logger.info("신규 재고 레코드를 생성했습니다. OrderType ID: {}", orderType.getId());
                    return savedInventory;
                });
    }

    /**
     * 모든 재고를 조회합니다.
     * @return 재고 목록
     */
    public List<Inventory> getAllInventories() {
        return inventoryRepository.findAll();
    }

    /**
     * 상태별 재고를 조회합니다.
     * @param status 재고 상태
     * @return 재고 목록
     */
    public List<Inventory> getInventoriesByStatus(InventoryStatus status) {
        return inventoryRepository.findByStatus(status);
    }

    /**
     * 재고를 저장합니다.
     * @param inventory 재고 객체
     */
    public void saveInventory(Inventory inventory) {
        inventoryRepository.save(inventory);
    }

    /**
     * 재고를 추가하거나 업데이트합니다.
     * @param inventoryCreateDTO 재고 생성 DTO
     */
    @Transactional
    public void addOrUpdateInventory(InventoryCreateDTO inventoryCreateDTO) {
        OrderType orderType = orderTypeService.getOrderTypeById(inventoryCreateDTO.getOrderTypeId());

        Optional<Inventory> existingInventoryOpt = inventoryRepository.findByOrderType(orderType);
        if (existingInventoryOpt.isPresent()) {
            Inventory existingInventory = existingInventoryOpt.get();

            existingInventory.setStatus(InventoryStatus.ACTIVE); // 활성 상태로 변경
            existingInventory.setQuantity(inventoryCreateDTO.getQuantity());
            inventoryRepository.save(existingInventory);
            logger.info("기존 재고 ID {}의 상태와 수량을 업데이트했습니다.", existingInventory.getId());
        } else {
            // 새 재고 추가
            Inventory inventory = new Inventory();
            inventory.setOrderType(orderType);
            inventory.setQuantity(inventoryCreateDTO.getQuantity());
            inventory.setStatus(InventoryStatus.ACTIVE); // 기본 상태 설정
            inventoryRepository.save(inventory);
            logger.info("새로운 재고를 추가했습니다. 재고 ID: {}", inventory.getId());
        }
    }

    /**
     * 재고 상태를 변경합니다.
     * @param id 재고 ID
     * @param status 변경할 상태
     * @throws InventoryNotFoundException 재고를 찾을 수 없는 경우
     */
    @Transactional
    public void changeInventoryStatus(Long id, InventoryStatus status) throws InventoryNotFoundException {
        Inventory inventory = getInventoryById(id);
        inventory.setStatus(status);
        inventoryRepository.save(inventory);
    }

    /**
     * 재고를 삭제합니다. (권장하지 않음 - 논리적 삭제 사용)
     * @param id 재고 ID
     * @throws InventoryNotFoundException 재고를 찾을 수 없는 경우
     */
    public void deleteInventory(Long id) throws InventoryNotFoundException {
        Inventory existingInventory = getInventoryById(id);
        inventoryRepository.delete(existingInventory);
    }

    /**
     * 특정 재고 거래를 폐기 처리합니다.
     * @param transaction 폐기할 재고 거래
     * @throws InventoryNotFoundException 재고를 찾을 수 없는 경우
     */
    @Transactional
    public void disposeInventory(StockTransaction transaction) throws InventoryNotFoundException {
        // 직접 Inventory 조회
        Inventory inventory = transaction.getInventory();

        if (inventory == null) {
            throw new InventoryNotFoundException("재고를 찾을 수 없습니다. 거래 ID: " + transaction.getId());
        }

        // 재고 상태를 DISPOSED로 변경
        inventory.setStatus(InventoryStatus.DISPOSED);
        inventoryRepository.save(inventory);
        logger.info("재고 ID {}를 DISPOSED 상태로 변경했습니다.", inventory.getId());

        // 거래 상태를 DISPOSED로 변경
        stockTransactionService.disposeTransaction(transaction);
        logger.info("거래 ID {}의 상태를 DISPOSED로 변경했습니다.", transaction.getId());
    }

    /**
     * 자동 폐기 처리를 수행합니다.
     */
    @Transactional
    public void processAutomaticDisposals() {
        LocalDateTime now = LocalDateTime.now();
        List<StockTransaction> transactionsToDispose = stockTransactionService.getPendingTransactionsBefore(now);

        logger.info("자동 폐기 처리를 시작합니다. 폐기할 거래 수: {}", transactionsToDispose.size());

        for (StockTransaction transaction : transactionsToDispose) {
            try {
                disposeInventory(transaction);
            } catch (InventoryNotFoundException e) {
                logger.error("폐기 처리 중 오류 발생: {}", e.getMessage());
            } catch (Exception e) {
                logger.error("폐기 처리 중 예기치 않은 오류 발생: {}", e.getMessage());
            }
        }

        logger.info("자동 폐기 처리 완료.");
    }

    /**
     * 폐기된 거래 목록을 조회합니다.
     * @return 폐기된 거래 목록
     */
    public List<StockTransaction> getDisposedTransactions() {
        return stockTransactionService.getTransactionsByStatus(TransactionStatus.DISPOSED);
    }
}
