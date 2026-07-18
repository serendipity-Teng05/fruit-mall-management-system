package com.example.fruitmallmanagementsystem.service;

import com.example.fruitmallmanagementsystem.entity.Permission;
import java.util.List;

public interface PermissionService {
    List<Permission> getAllPermissions();
    void addPermission(Permission permission);
    void updatePermission(Permission permission);
    void deletePermission(Long permissionId);
}