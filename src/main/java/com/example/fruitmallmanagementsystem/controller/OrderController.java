package com.example.fruitmallmanagementsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.fruitmallmanagementsystem.common.Result;
import com.example.fruitmallmanagementsystem.config.RequirePermission;
import com.example.fruitmallmanagementsystem.dto.OrderDto;
import com.example.fruitmallmanagementsystem.entity.Order;
import com.example.fruitmallmanagementsystem.entity.User;
import com.example.fruitmallmanagementsystem.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/sys_order")
@RequirePermission("ORDER_MANAGE")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 创建订单
     */
    @PostMapping("/create")
    public Result create(@RequestBody OrderDto orderDTO,
                         @RequestAttribute("currentUser") User currentUser) {
        if (!StringUtils.hasText(orderDTO.getRequestId())) {
            orderDTO.setRequestId("ADMIN_" + UUID.randomUUID().toString().replace("-", ""));
        }
        return Result.success(orderService.createOrder(orderDTO, currentUser.getId()));
    }

    /**
     * 订单列表
     */
    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize,
                       @RequestParam(required = false) String orderNo,
                       @RequestParam(required = false) Integer status) {

        pageNum = Math.max(1, pageNum);
        pageSize = Math.min(100, Math.max(1, pageSize));
        Page<Order> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getArchived, 0);

        if (status != null && status != -1) {
            wrapper.eq(Order::getStatus, status);
        }

        if (StringUtils.hasText(orderNo)) {
            wrapper.like(Order::getOrderNo, orderNo);
        }

        wrapper.orderByDesc(Order::getCreateTime);

        orderService.page(page, wrapper);
        return Result.success(page);
    }

    /**
     * 更新订单状态
     */
    @PostMapping("/status")
    public Result updateStatus(@RequestParam String orderNo, @RequestParam Integer status) {
        orderService.changeStatus(orderNo, status);
        return Result.success();
    }

    /**
     * 删除订单
     */
    @PostMapping("/delete")
    public Result delete(@RequestParam String orderNo) {
        orderService.deleteCancelledOrder(orderNo);
        return Result.success();
    }

    /**
     * 获取订单详情（包含订单基础信息和商品明细）
     */
    @GetMapping({"/detail", "/detail/{orderNo}"})
    public Result getOrderDetail(@PathVariable(value = "orderNo", required = false) String pathOrderNo,
                                 @RequestParam(value = "orderNo", required = false) String queryOrderNo) {
        String orderNo = StringUtils.hasText(queryOrderNo) ? queryOrderNo : pathOrderNo;
        if (!StringUtils.hasText(orderNo)) {
            return Result.error("orderNo不能为空");
        }

        OrderDto orderDTO = orderService.getOrderWithItems(orderNo);
        if (orderDTO == null) {
            return Result.error("未找到该订单信息");
        }

        return Result.success(orderDTO);
    }
}
