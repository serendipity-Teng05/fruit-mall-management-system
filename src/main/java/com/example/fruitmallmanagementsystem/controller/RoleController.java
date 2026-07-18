package com.example.fruitmallmanagementsystem.controller;

import com.example.fruitmallmanagementsystem.common.Result;
import com.example.fruitmallmanagementsystem.config.RequirePermission;
import com.example.fruitmallmanagementsystem.entity.Role;
import com.example.fruitmallmanagementsystem.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/sys_role")
@RequirePermission("ROLE_MANAGE")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @GetMapping
    public Result<List<Role>> getAllRoles() {
        return Result.success(roleService.getAllRoles());
    }

    @GetMapping("/options")
    @RequirePermission({"USER_MANAGE", "USER_ROLE_ASSIGN", "ROLE_MANAGE"})
    public Result<List<Role>> getRoleOptions() {
        return Result.success(roleService.getAllRoles());
    }

    @GetMapping("/{roleId}")
    public Result<Role> getRole(@PathVariable Long roleId) {
        return Result.success(roleService.getRoleById(roleId));
    }

    @PostMapping
    public Result<String> addRole(@RequestBody Role role) {
        roleService.addRole(role);
        return Result.success("角色添加成功");
    }

    @PutMapping
    public Result<String> updateRole(@RequestBody Role role) {
        roleService.updateRole(role);
        return Result.success("角色更新成功");
    }

    @DeleteMapping("/{roleId}")
    public Result<String> deleteRole(@PathVariable Long roleId) {
        roleService.deleteRole(roleId);
        return Result.success("角色删除成功");
    }
}
