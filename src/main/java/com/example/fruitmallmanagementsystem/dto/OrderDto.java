package com.example.fruitmallmanagementsystem.dto;

import com.example.fruitmallmanagementsystem.entity.Order;
import com.example.fruitmallmanagementsystem.entity.OrderItem;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true) // 推荐加上，表示 hashCode 也包含父类字段
public class OrderDto extends Order {

    // ✅ 继承了 Order，所以 orderNo, status, totalAmount, createTime 等统统都有了！
    // 不需要手动再写一遍

    // 🚀 唯一需要额外添加的字段：商品明细列表
    private List<OrderItem> items;
}
