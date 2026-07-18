package com.example.fruitmallmanagementsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.fruitmallmanagementsystem.common.Result;
import com.example.fruitmallmanagementsystem.config.RequirePermission;
import com.example.fruitmallmanagementsystem.dto.PaymentCreateResult;
import com.example.fruitmallmanagementsystem.entity.Payment;
import com.example.fruitmallmanagementsystem.entity.User;
import com.example.fruitmallmanagementsystem.service.PaymentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/capabilities")
    public Result<?> capabilities() {
        return Result.success(paymentService.getCapabilities());
    }

    @PostMapping("/create")
    public Result<PaymentCreateResult> create(@RequestParam String orderNo,
                                              @RequestParam(defaultValue = "DEMO") String channel,
                                              @RequestAttribute("currentUser") User currentUser) {
        return Result.success(paymentService.createPayment(orderNo, currentUser.getId(), channel));
    }

    @PostMapping("/demo/confirm")
    public Result<Payment> confirm(@RequestParam String paymentNo,
                                   @RequestAttribute("currentUser") User currentUser) {
        return Result.success(paymentService.confirmDemoPayment(paymentNo, currentUser.getId()));
    }

    @GetMapping("/list")
    @RequirePermission("PAYMENT_MANAGE")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer pageNum,
                          @RequestParam(defaultValue = "10") Integer pageSize,
                          @RequestParam(required = false) String orderNo,
                          @RequestParam(required = false) Integer status) {
        return Result.success(paymentService.pageList(pageNum, pageSize, orderNo, status));
    }

    @GetMapping("/status")
    public Result<Payment> status(@RequestParam String orderNo,
                                  @RequestAttribute("currentUser") User currentUser) {
        return Result.success(paymentService.getLatestForUser(orderNo, currentUser.getId()));
    }

    @GetMapping("/admin/status")
    @RequirePermission("PAYMENT_MANAGE")
    public Result<Payment> adminStatus(@RequestParam String orderNo) {
        return Result.success(paymentService.getOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderNo, orderNo)
                .orderByDesc(Payment::getCreateTime)
                .last("LIMIT 1")));
    }
}
