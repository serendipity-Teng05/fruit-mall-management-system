package com.example.fruitmallmanagementsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import java.math.BigDecimal; // 必须导入这个
import java.time.LocalDateTime;

@Data
@TableName("sys_product")
public class Product {
    // 对应 id (bigint)
    @TableId(type = IdType.AUTO)
    private Long id;

    // 对应 name (varchar)
    private String name;

    // 对应 category (varchar)
    private String category;

    // 对应 price (decimal) -> 必须用 BigDecimal
    private BigDecimal price;

    // 对应 stock (int)
    private Integer stock;

    // 对应 unit (varchar)
    private String unit;

    // 对应 image (varchar) -> 你的数据库字段叫 image
    private String image;

    private String description;

    private String origin;

    private Integer salesCount;

    // 对应 status (int)
    private Integer status;

    // 对应 version (int)
    @Version
    private Integer version;

    private Integer stockThreshold;

    // 对应 create_time (datetime)
    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

