package com.example.fruitmallmanagementsystem.controller;

import com.example.fruitmallmanagementsystem.common.Result;
import com.example.fruitmallmanagementsystem.config.RequirePermission;
import com.example.fruitmallmanagementsystem.dto.UserRoleAssignDTO;
import com.example.fruitmallmanagementsystem.entity.Role;
import com.example.fruitmallmanagementsystem.entity.User;
import com.example.fruitmallmanagementsystem.entity.UserRole;
import com.example.fruitmallmanagementsystem.service.RoleService;
import com.example.fruitmallmanagementsystem.service.UserRoleService;
import com.example.fruitmallmanagementsystem.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/sys_user_role")
@RequirePermission("USER_ROLE_ASSIGN")
public class UserRoleController {
    private final UserRoleService userRoleService;
    private final UserService userService;
    private final RoleService roleService;

    public UserRoleController(UserRoleService userRoleService, UserService userService, RoleService roleService) {
        this.userRoleService = userRoleService;
        this.userService = userService;
        this.roleService = roleService;
    }

    @GetMapping("/user/{userId}")
    public Result<List<UserRole>> getRolesByUser(@PathVariable Long userId) {
        return Result.success(userRoleService.getRolesByUserId(userId));
    }

    @GetMapping("/users")
    public Result<?> getAssignableUsers(@RequestParam(defaultValue = "1") Integer pageNum,
                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                        @RequestParam(required = false) String keyword) {
        return Result.success(userService.pageWithRoles(pageNum, pageSize, keyword, true));
    }

    @GetMapping("/roles")
    public Result<List<Role>> getAssignableRoles() {
        return Result.success(roleService.getAllRoles());
    }

    @PostMapping
    public Result<String> addUserRole(@RequestBody UserRole relation,
                                      @RequestAttribute("currentUser") User operator) {
        List<Long> roleIds = new ArrayList<>(userRoleService.getRolesByUserId(relation.getUserId())
                .stream().map(UserRole::getRoleId).toList());
        roleIds.add(relation.getRoleId());
        userRoleService.assignRolesByOperator(relation.getUserId(), roleIds, operator);
        return Result.success("用户角色添加成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> deleteUserRole(@PathVariable Long id,
                                         @RequestAttribute("currentUser") User operator) {
        UserRole relation = userRoleService.getById(id);
        if (relation == null) {
            throw new IllegalArgumentException("用户角色关系不存在");
        }
        List<Long> roleIds = userRoleService.getRolesByUserId(relation.getUserId()).stream()
                .filter(item -> !Objects.equals(item.getId(), id))
                .map(UserRole::getRoleId).toList();
        userRoleService.assignRolesByOperator(relation.getUserId(), roleIds, operator);
        return Result.success("用户角色删除成功");
    }

    @PostMapping("/assign")
    public Result<String> assignRoles(@RequestBody UserRoleAssignDTO dto,
                                      @RequestAttribute("currentUser") User operator) {
        userRoleService.assignRolesByOperator(dto.getUserId(), dto.getRoleIds(), operator);
        return Result.success("用户角色分配成功");
    }
}
