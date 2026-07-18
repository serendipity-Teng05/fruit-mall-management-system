package com.example.fruitmallmanagementsystem.service;

import com.example.fruitmallmanagementsystem.entity.RolePermission;
import com.example.fruitmallmanagementsystem.entity.User;
import java.util.List;

public interface RolePermissionService {
    RolePermission getById(Long id);

    List<RolePermission> getPermissionsByRoleId(Long roleId);

    void addRolePermission(RolePermission rp);

    void deleteRolePermission(Long id);

    void assignPermissions(Long roleId, List<Long> permissionIds);

    void assignPermissionsByOperator(Long roleId, List<Long> permissionIds, User operator);
}
