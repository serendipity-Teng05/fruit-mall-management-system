package com.example.fruitmallmanagementsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sys_payment")
public class Payment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String paymentNo;
    private String orderNo;
    private Long userId;
    private String channel;
    private BigDecimal amount;
    /** 0-待支付，1-已支付，2-已关闭，3-支付失败 */
    private Integer status;
    private String providerTradeNo;
    private LocalDateTime createTime;
    private LocalDateTime paidTime;
}
