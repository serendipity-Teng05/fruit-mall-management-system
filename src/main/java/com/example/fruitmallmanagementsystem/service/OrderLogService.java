package com.example.fruitmallmanagementsystem.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.fruitmallmanagementsystem.entity.OrderLog;

import java.util.List;

public interface OrderLogService extends IService<OrderLog> {
    Page<OrderLog> pageList(Integer pageNum, Integer pageSize, String orderNo);

    List<OrderLog> getLogsByOrderNo(String orderNo);

    void addOrderLog(OrderLog log);
}
