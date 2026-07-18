package com.example.fruitmallmanagementsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.fruitmallmanagementsystem.dto.OrderDto;
import com.example.fruitmallmanagementsystem.dto.OrderCreateResult;
import com.example.fruitmallmanagementsystem.entity.Order;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.time.LocalDateTime;

public interface OrderService extends IService<Order> {
    // 下单核心方法
    OrderCreateResult createOrder(OrderDto orderDTO, Long userId);

    // 在 OrderService.java 中添加：
    OrderDto getOrderWithItems(String orderNo);

    OrderDto getOrderWithItemsForUser(String orderNo, Long userId);

    Page<Order> pageMyOrders(Long userId, Integer pageNum, Integer pageSize, String keyword, Integer status);

    void changeUserOrderStatus(String orderNo, Long userId, Integer targetStatus);

    boolean markPaid(String orderNo, LocalDateTime paymentTime);

    void changeStatus(String orderNo, Integer targetStatus);

    void deleteCancelledOrder(String orderNo);

    int cancelExpiredOrders();
}
