package com.example.fruitmallmanagementsystem.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_log")
public class SysLog {
    @TableId
    private Long logId;
    private Long userId;
    private String username;
    private String action;
    private String module;
    private String remark;
    private String ipAddress;
    private String httpMethod;
    private String requestUri;
    private String requestId;
    private Integer success;
    private Long durationMs;
    private LocalDateTime createTime;
}
