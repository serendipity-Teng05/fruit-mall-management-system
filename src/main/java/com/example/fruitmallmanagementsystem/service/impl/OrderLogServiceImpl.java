package com.example.fruitmallmanagementsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.fruitmallmanagementsystem.entity.OrderLog;
import com.example.fruitmallmanagementsystem.mapper.OrderLogMapper;
import com.example.fruitmallmanagementsystem.service.OrderLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class OrderLogServiceImpl extends ServiceImpl<OrderLogMapper, OrderLog> implements OrderLogService {

    @Override
    public Page<OrderLog> pageList(Integer pageNum, Integer pageSize, String orderNo) {
        Page<OrderLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OrderLog> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(orderNo)) {
            wrapper.like(OrderLog::getOrderNo, orderNo.trim());
        }

        wrapper.orderByDesc(OrderLog::getChangeTime);
        return page(page, wrapper);
    }

    @Override
    public List<OrderLog> getLogsByOrderNo(String orderNo) {
        return list(new LambdaQueryWrapper<OrderLog>()
                .eq(OrderLog::getOrderNo, orderNo)
                .orderByDesc(OrderLog::getChangeTime));
    }

    @Override
    public void addOrderLog(OrderLog log) {
        save(log);
    }
}
