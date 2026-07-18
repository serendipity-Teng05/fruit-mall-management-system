package com.example.fruitmallmanagementsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.fruitmallmanagementsystem.common.Result;
import com.example.fruitmallmanagementsystem.config.RequirePermission;
import com.example.fruitmallmanagementsystem.entity.Product;
import com.example.fruitmallmanagementsystem.entity.vo.DashboardVO;
import com.example.fruitmallmanagementsystem.mapper.OrderMapper;
import com.example.fruitmallmanagementsystem.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
@RequirePermission(roles = {"ADMIN", "STAFF"})
public class DashboardController {
    private final OrderMapper orderMapper;
    private final ProductService productService;

    public DashboardController(OrderMapper orderMapper, ProductService productService) {
        this.orderMapper = orderMapper;
        this.productService = productService;
    }

    @GetMapping("/data")
    public Result<DashboardVO> data() {
        DashboardVO vo = new DashboardVO();
        vo.setProductCount(Math.toIntExact(productService.count(new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1))));
        List<Product> lowStock = productService.list(new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1)
                .apply("stock <= COALESCE(stock_threshold, 10)")
                .orderByAsc(Product::getStock)
                .last("LIMIT 20"));
        vo.setLowStockList(lowStock);
        vo.setLowStockCount(lowStock.size());
        vo.setPendingOrders(Math.toIntExact(orderMapper.countByStatus(1)));
        vo.setTotalSales(defaultZero(orderMapper.totalSales()));

        List<Map<String, Object>> statusData = new ArrayList<>();
        String[] names = {"待支付", "待发货", "待收货", "已完成", "已取消"};
        for (int status = 0; status < names.length; status++) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", names[status]);
            item.put("value", orderMapper.countByStatus(status));
            statusData.add(item);
        }
        vo.setStatusData(statusData);

        List<String> days = new ArrayList<>();
        List<BigDecimal> sales = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            days.add(day.format(formatter));
            sales.add(defaultZero(orderMapper.salesBetween(day.atStartOfDay(), day.plusDays(1).atStartOfDay())));
        }
        vo.setChartDays(days);
        vo.setChartSales(sales);
        return Result.success(vo);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
