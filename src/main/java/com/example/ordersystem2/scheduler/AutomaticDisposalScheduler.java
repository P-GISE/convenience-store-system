// src/main/java/com/example/ordersystem2/scheduler/AutomaticDisposalScheduler.java
package com.example.ordersystem2.scheduler;

import com.example.ordersystem2.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class AutomaticDisposalScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AutomaticDisposalScheduler.class);

    @Autowired
    private InventoryService inventoryService;

    /**
     * 매 시간 정각에 자동 폐기 처리를 실행합니다.
     * 크론 표현식: 초 분 시 일 월 요일
     * 0 0 * * * ? -> 매 시간 정각
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void runAutomaticDisposals() {
        logger.info("자동 폐기 스케줄러 실행 시작.");
        inventoryService.processAutomaticDisposals();
        logger.info("자동 폐기 스케줄러 실행 완료.");
    }

    /**
     * 테스트를 위해 매 5분마다 실행하도록 설정 (운영 시 제거)
     */
    /*
    @Scheduled(cron = "0 0/5 * * * ?")
    public void runAutomaticDisposals() {
        inventoryService.processAutomaticDisposals();
    }
    */
}
