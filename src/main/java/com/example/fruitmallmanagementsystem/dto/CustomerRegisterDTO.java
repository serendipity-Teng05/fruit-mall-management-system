package com.example.fruitmallmanagementsystem.dto;

import lombok.Data;

@Data
public class CustomerRegisterDTO {
    private String username;
    private String password;
    private String realName;
    private String phone;
    private String captcha;
}
