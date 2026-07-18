package com.example.fruitmallmanagementsystem.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_permission")
public class Permission {
    @TableId
    private Long permissionId;
    private String permissionName;
    private String permissionCode;
    private String permissionType; // MENU / API
    private Long parentId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}