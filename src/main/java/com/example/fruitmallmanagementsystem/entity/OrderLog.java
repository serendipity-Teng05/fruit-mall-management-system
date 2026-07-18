package com.example.fruitmallmanagementsystem.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_order_log")
public class OrderLog {
    @TableId
    private Long logId;
    private String orderNo;
    private String statusBefore;
    private String statusAfter;
    private String remark;
    private Long operatorId;
    private String source;
    private String requestId;
    private LocalDateTime changeTime;
}
