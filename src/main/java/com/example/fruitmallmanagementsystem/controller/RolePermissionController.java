package com.example.fruitmallmanagementsystem.controller;

import com.example.fruitmallmanagementsystem.common.Result;
import com.example.fruitmallmanagementsystem.config.RequirePermission;
import com.example.fruitmallmanagementsystem.dto.RolePermissionAssignDTO;
import com.example.fruitmallmanagementsystem.entity.Permission;
import com.example.fruitmallmanagementsystem.entity.Role;
import com.example.fruitmallmanagementsystem.entity.RolePermission;
import com.example.fruitmallmanagementsystem.entity.User;
import com.example.fruitmallmanagementsystem.service.PermissionService;
import com.example.fruitmallmanagementsystem.service.RolePermissionService;
import com.example.fruitmallmanagementsystem.service.RoleService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/sys_role_permission")
@RequirePermission("ROLE_PERMISSION")
public class RolePermissionController {
    private final RolePermissionService rolePermissionService;
    private final RoleService roleService;
    private final PermissionService permissionService;

    public RolePermissionController(RolePermissionService rolePermissionService,
                                    RoleService roleService,
                                    PermissionService permissionService) {
        this.rolePermissionService = rolePermissionService;
        this.roleService = roleService;
        this.permissionService = permissionService;
    }

    @GetMapping("/role/{roleId}")
    public Result<List<RolePermission>> getPermissionsByRole(@PathVariable Long roleId) {
        return Result.success(rolePermissionService.getPermissionsByRoleId(roleId));
    }

    @GetMapping("/roles")
    public Result<List<Role>> getRoleOptions() {
        return Result.success(roleService.getAllRoles());
    }

    @GetMapping("/permissions")
    public Result<List<Permission>> getPermissionOptions() {
        return Result.success(permissionService.getAllPermissions());
    }

    @PostMapping
    public Result<String> addRolePermission(@RequestBody RolePermission relation,
                                            @RequestAttribute("currentUser") User operator) {
        List<Long> permissionIds = new ArrayList<>(rolePermissionService
                .getPermissionsByRoleId(relation.getRoleId()).stream()
                .map(RolePermission::getPermissionId).toList());
        permissionIds.add(relation.getPermissionId());
        rolePermissionService.assignPermissionsByOperator(relation.getRoleId(), permissionIds, operator);
        return Result.success("角色权限添加成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> deleteRolePermission(@PathVariable Long id,
                                               @RequestAttribute("currentUser") User operator) {
        RolePermission relation = rolePermissionService.getById(id);
        if (relation == null) {
            throw new IllegalArgumentException("角色权限关系不存在");
        }
        List<Long> permissionIds = rolePermissionService.getPermissionsByRoleId(relation.getRoleId()).stream()
                .filter(item -> !Objects.equals(item.getId(), id))
                .map(RolePermission::getPermissionId).toList();
        rolePermissionService.assignPermissionsByOperator(relation.getRoleId(), permissionIds, operator);
        return Result.success("角色权限删除成功");
    }

    @PostMapping("/assign")
    public Result<String> assignPermissions(@RequestBody RolePermissionAssignDTO dto,
                                            @RequestAttribute("currentUser") User operator) {
        rolePermissionService.assignPermissionsByOperator(dto.getRoleId(), dto.getPermissionIds(), operator);
        return Result.success("角色权限分配成功");
    }
}
