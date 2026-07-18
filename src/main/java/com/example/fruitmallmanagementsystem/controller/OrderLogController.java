package com.example.fruitmallmanagementsystem.controller;

import com.example.fruitmallmanagementsystem.common.Result;
import com.example.fruitmallmanagementsystem.config.RequirePermission;
import com.example.fruitmallmanagementsystem.entity.OrderLog;
import com.example.fruitmallmanagementsystem.service.OrderLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sys_order_log")
@RequirePermission("ORDER_LOG")
public class OrderLogController {

    @Autowired
    private OrderLogService orderLogService;

    @GetMapping("/list")
    public Result list(@RequestParam(required = false) String orderNo,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(orderLogService.pageList(pageNum, pageSize, orderNo));
    }

    @GetMapping("/order/{orderNo}")
    public Result getLogsByOrder(@PathVariable String orderNo) {
        return Result.success(orderLogService.getLogsByOrderNo(orderNo));
    }

}
