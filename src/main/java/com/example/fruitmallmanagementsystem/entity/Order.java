package com.example.fruitmallmanagementsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sys_order")
public class Order {
    // ⚠️ 注意：根据截图，order_no 是主键 (Key 图标)，且是 varchar 类型
    // 这里 type = IdType.ASSIGN_ID (雪花算法生成) 或者 IdType.INPUT (手动输入)
    // 建议先用 INPUT 或 ASSIGN_ID，类型必须是 String
    @TableId(value = "order_no", type = IdType.INPUT)
    private String orderNo;

    // 对应 user_id (bigint)
    private Long userId;

    private String requestId;

    // 对应 total_amount (decimal)
    private BigDecimal totalAmount;

    // 对应 status (int)
    private Integer status;

    // 对应 customer_name (varchar)
    private String customerName;

    // 对应 customer_phone (varchar)
    private String customerPhone;

    // 对应 address (varchar)
    private String address;

    // 对应 create_time (datetime)
    private LocalDateTime createTime;

    /** 支付时间 */
    private LocalDateTime paymentTime;

    /** 发货时间 */
    private LocalDateTime deliveryTime;

    /** 收货时间 */
    private LocalDateTime receiveTime;

    /** 待支付订单失效时间 */
    private LocalDateTime expireTime;

    /** 取消时间 */
    private LocalDateTime cancelTime;

    private Integer archived;
}
