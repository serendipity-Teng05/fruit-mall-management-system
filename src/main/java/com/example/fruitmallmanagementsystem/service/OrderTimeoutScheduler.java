package com.example.fruitmallmanagementsystem.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "app.order.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class OrderTimeoutScheduler {
    private final OrderService orderService;
    private final DatabaseSchedulerLock schedulerLock;

    public OrderTimeoutScheduler(OrderService orderService, DatabaseSchedulerLock schedulerLock) {
        this.orderService = orderService;
        this.schedulerLock = schedulerLock;
    }

    @Scheduled(
            fixedDelayString = "${app.order.timeout-scan-millis:60000}",
            initialDelayString = "${app.order.timeout-initial-delay-millis:30000}"
    )
    public void closeExpiredOrders() {
        String lockName = "close-expired-orders";
        if (!schedulerLock.tryAcquire(lockName, Duration.ofMinutes(10))) return;
        try {
            orderService.cancelExpiredOrders();
        } finally {
            schedulerLock.release(lockName);
        }
    }
}
