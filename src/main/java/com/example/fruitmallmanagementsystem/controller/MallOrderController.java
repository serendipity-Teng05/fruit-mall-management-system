package com.example.fruitmallmanagementsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.fruitmallmanagementsystem.common.Result;
import com.example.fruitmallmanagementsystem.dto.MallOrderCreateDTO;
import com.example.fruitmallmanagementsystem.dto.OrderDto;
import com.example.fruitmallmanagementsystem.dto.OrderCreateResult;
import com.example.fruitmallmanagementsystem.entity.Order;
import com.example.fruitmallmanagementsystem.entity.OrderItem;
import com.example.fruitmallmanagementsystem.entity.User;
import com.example.fruitmallmanagementsystem.entity.UserAddress;
import com.example.fruitmallmanagementsystem.service.OrderService;
import com.example.fruitmallmanagementsystem.service.UserAddressService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mall/orders")
public class MallOrderController {
    private final OrderService orderService;
    private final UserAddressService addressService;

    public MallOrderController(OrderService orderService, UserAddressService addressService) {
        this.orderService = orderService;
        this.addressService = addressService;
    }

    @PostMapping
    public Result<OrderCreateResult> create(@RequestBody MallOrderCreateDTO request,
                                              @RequestAttribute("currentUser") User currentUser) {
        if (request == null || request.getAddressId() == null) {
            return Result.error("请选择收货地址");
        }
        UserAddress address = addressService.getForUser(request.getAddressId(), currentUser.getId());
        if (address == null) {
            return Result.error("收货地址不存在");
        }
        OrderDto order = new OrderDto();
        order.setRequestId(request.getRequestId());
        order.setCustomerName(address.getRecipientName());
        order.setCustomerPhone(address.getPhone());
        order.setAddress(address.toFullAddress());
        List<OrderItem> items = request.getItems() == null ? List.of() : request.getItems().stream()
                .map(item -> {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setProductId(item.getProductId());
                    orderItem.setCount(item.getCount());
                    return orderItem;
                })
                .toList();
        order.setItems(items);
        return Result.success(orderService.createOrder(order, currentUser.getId()));
    }

    @GetMapping
    public Result<Page<Order>> list(@RequestAttribute("currentUser") User currentUser,
                                    @RequestParam(defaultValue = "1") Integer pageNum,
                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) Integer status) {
        return Result.success(orderService.pageMyOrders(
                currentUser.getId(), pageNum, pageSize, keyword, status));
    }

    @GetMapping("/{orderNo}")
    public Result<OrderDto> detail(@PathVariable String orderNo,
                                   @RequestAttribute("currentUser") User currentUser) {
        OrderDto detail = orderService.getOrderWithItemsForUser(orderNo, currentUser.getId());
        if (detail == null) {
            return Result.error("订单不存在");
        }
        return Result.success(detail);
    }

    @PostMapping("/{orderNo}/cancel")
    public Result<Void> cancel(@PathVariable String orderNo,
                               @RequestAttribute("currentUser") User currentUser) {
        orderService.changeUserOrderStatus(orderNo, currentUser.getId(), 4);
        return Result.success();
    }

    @PostMapping("/{orderNo}/confirm-receipt")
    public Result<Void> confirmReceipt(@PathVariable String orderNo,
                                       @RequestAttribute("currentUser") User currentUser) {
        orderService.changeUserOrderStatus(orderNo, currentUser.getId(), 3);
        return Result.success();
    }
}
