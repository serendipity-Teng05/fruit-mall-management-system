package com.example.fruitmallmanagementsystem.dto;

import lombok.Data;

import java.util.List;

@Data
public class RolePermissionAssignDTO {
    private Long roleId;
    private List<Long> permissionIds;
}