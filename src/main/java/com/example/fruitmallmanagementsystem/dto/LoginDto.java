package com.example.fruitmallmanagementsystem.dto;

import lombok.Data;

@Data
public class LoginDto {
    // 登录只需要这三个参数
    private String username;
    private String password;
    private String captcha; // 验证码
}