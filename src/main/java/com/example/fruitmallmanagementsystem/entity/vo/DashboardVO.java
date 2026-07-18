package com.example.fruitmallmanagementsystem.entity.vo;

import com.example.fruitmallmanagementsystem.entity.Product;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class DashboardVO {
    // 1. 顶部统计卡片
    private BigDecimal totalSales;    // 总销售额
    private Integer pendingOrders;    // 待处理订单
    private Integer lowStockCount;    // 库存预警数量
    private Integer productCount;     // 商品总数

    // 2. 图表数据
    private List<String> chartDays;           // 周趋势-X轴 (日期)
    private List<BigDecimal> chartSales;      // 周趋势-Y轴 (金额)
    private List<Map<String, Object>> statusData; // 饼图数据

    // 3. 底部预警列表 (新增的核心字段)
    private List<Product> lowStockList;
}