package com.example.fruitmallmanagementsystem.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserRoleAssignDTO {
    private Long userId;
    private List<Long> roleIds;
}