package com.example.fruitmallmanagementsystem.dto;

import lombok.Data;

@Data
public class PasswordDto {
    private Long id;          // 用户ID
    private String oldPassword; // 旧密码 (用于验证)
    private String newPassword; // 新密码
}