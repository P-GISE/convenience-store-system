// src/main/java/com/example/ordersystem2/scheduler/InventoryScheduler.java
package com.example.ordersystem2.scheduler;

import com.example.ordersystem2.model.Inventory;
import com.example.ordersystem2.model.InventoryStatus;
import com.example.ordersystem2.exception.InventoryNotFoundException;
import com.example.ordersystem2.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class InventoryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(InventoryScheduler.class);

    @Autowired
    private InventoryService inventoryService;

    /**
     * 매일 자정에 실행되어 만료된 발주를 폐기처리로 변경합니다.
     */
    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정
    @Transactional
    public void disposeExpiredInventories() {
        // 만료 조건: 완료 날짜가 현재 시간보다 30일 이전인 경우
        LocalDateTime expirationThreshold = LocalDateTime.now().minusDays(30);
        List<Inventory> completedInventories = inventoryService.getInventoriesByStatus(InventoryStatus.COMPLETED);

        for (Inventory inventory : completedInventories) {
            if (inventory.getCompletionDate().isBefore(expirationThreshold)) {
                try {
                    inventoryService.changeInventoryStatus(inventory.getId(), InventoryStatus.DISPOSED);
                    logger.info("발주 ID {}가 폐기처리되었습니다.", inventory.getId());
                } catch (InventoryNotFoundException e) {
                    logger.error("발주 ID {}을(를) 찾을 수 없어 폐기처리할 수 없습니다.", inventory.getId(), e);
                    // 필요에 따라 추가적인 조치를 취할 수 있습니다.
                }
            }
        }
    }
}
