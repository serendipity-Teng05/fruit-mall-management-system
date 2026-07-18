package com.example.fruitmallmanagementsystem.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.fruitmallmanagementsystem.entity.Payment;
import com.example.fruitmallmanagementsystem.dto.PaymentCreateResult;
import com.example.fruitmallmanagementsystem.dto.PaymentCapabilities;

public interface PaymentService extends IService<Payment> {
    Payment createDemoPayment(String orderNo, Long userId);

    PaymentCreateResult createPayment(String orderNo, Long userId, String channel);

    Payment confirmDemoPayment(String paymentNo, Long userId);

    Page<Payment> pageList(Integer pageNum, Integer pageSize, String orderNo, Integer status);

    Payment getLatestForUser(String orderNo, Long userId);

    PaymentCapabilities getCapabilities();
}
