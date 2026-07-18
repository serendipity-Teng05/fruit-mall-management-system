package com.example.fruitmallmanagementsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("sys_order_item")
public class OrderItem {
    // 对应 id (bigint)
    @TableId(type = IdType.AUTO)
    private Long id;

    // 对应 order_no (varchar) -> 关联 sys_order 的主键
    private String orderNo;

    // 对应 product_id (bigint)
    private Long productId;

    // 对应 product_name (varchar)
    private String productName;

    private String productImage;

    private String productUnit;

    // 对应 price (decimal)
    private BigDecimal price;

    // 对应 count (int)
    private Integer count;
}
