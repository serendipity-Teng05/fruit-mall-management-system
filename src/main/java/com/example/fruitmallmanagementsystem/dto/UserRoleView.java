package com.example.fruitmallmanagementsystem.dto;

import lombok.Data;

@Data
public class UserRoleView {
    private Long userId;
    private Long roleId;
    private String roleCode;
    private String roleName;
}
