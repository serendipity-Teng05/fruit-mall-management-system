package com.example.fruitmallmanagementsystem.controller;

import com.example.fruitmallmanagementsystem.common.Result;
import com.example.fruitmallmanagementsystem.config.RequirePermission;
import com.example.fruitmallmanagementsystem.entity.Permission;
import com.example.fruitmallmanagementsystem.service.PermissionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sys_permission")
@RequirePermission(value = "ROLE_PERMISSION", roles = "ADMIN")
public class PermissionController {
    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    public Result<List<Permission>> getAllPermissions() {
        return Result.success(permissionService.getAllPermissions());
    }

    @PostMapping
    public Result<String> addPermission(@RequestBody Permission permission) {
        permissionService.addPermission(permission);
        return Result.success("权限添加成功");
    }

    @PutMapping
    public Result<String> updatePermission(@RequestBody Permission permission) {
        permissionService.updatePermission(permission);
        return Result.success("权限更新成功");
    }

    @DeleteMapping("/{permissionId}")
    public Result<String> deletePermission(@PathVariable Long permissionId) {
        permissionService.deletePermission(permissionId);
        return Result.success("权限删除成功");
    }
}
