package com.example.fruitmallmanagementsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.fruitmallmanagementsystem.entity.Permission;
import com.example.fruitmallmanagementsystem.entity.Role;
import com.example.fruitmallmanagementsystem.entity.RolePermission;
import com.example.fruitmallmanagementsystem.entity.User;
import com.example.fruitmallmanagementsystem.mapper.PermissionMapper;
import com.example.fruitmallmanagementsystem.mapper.RoleMapper;
import com.example.fruitmallmanagementsystem.mapper.RolePermissionMapper;
import com.example.fruitmallmanagementsystem.mapper.UserMapper;
import com.example.fruitmallmanagementsystem.service.RolePermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

@Service
public class RolePermissionServiceImpl extends ServiceImpl<RolePermissionMapper, RolePermission>
        implements RolePermissionService {
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final UserMapper userMapper;

    public RolePermissionServiceImpl(RoleMapper roleMapper, PermissionMapper permissionMapper,
                                     UserMapper userMapper) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.userMapper = userMapper;
    }

    @Override
    public RolePermission getById(Long id) {
        return super.getById(id);
    }

    @Override
    public List<RolePermission> getPermissionsByRoleId(Long roleId) {
        return list(new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, roleId)
                .orderByAsc(RolePermission::getPermissionId));
    }

    @Override
    public void addRolePermission(RolePermission relation) {
        if (relation == null || relation.getRoleId() == null || relation.getPermissionId() == null) {
            throw new IllegalArgumentException("角色和权限不能为空");
        }
        List<Long> ids = new ArrayList<>(getPermissionsByRoleId(relation.getRoleId()).stream()
                .map(RolePermission::getPermissionId).toList());
        ids.add(relation.getPermissionId());
        assignPermissions(relation.getRoleId(), ids);
    }

    @Override
    public void deleteRolePermission(Long id) {
        RolePermission relation = id == null ? null : getById(id);
        if (relation == null) {
            throw new IllegalArgumentException("角色权限关系不存在");
        }
        List<Long> ids = getPermissionsByRoleId(relation.getRoleId()).stream()
                .filter(item -> !Objects.equals(item.getId(), id))
                .map(RolePermission::getPermissionId).toList();
        assignPermissions(relation.getRoleId(), ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        assignPermissionsInternal(roleId, permissionIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissionsByOperator(Long roleId, List<Long> permissionIds, User operator) {
        Role role = requireRole(roleId);
        if ("ADMIN".equalsIgnoreCase(role.getRoleCode())
                && (operator == null || !"admin".equalsIgnoreCase(operator.getUsername()))) {
            throw new IllegalArgumentException("只有超级管理员可以修改 ADMIN 角色权限");
        }
        if ("CUSTOMER".equalsIgnoreCase(role.getRoleCode())
                && permissionIds != null && !permissionIds.isEmpty()) {
            throw new IllegalArgumentException("顾客端使用数据归属校验，不应授予后台管理权限");
        }
        if (operator == null) {
            throw new IllegalArgumentException("登录状态无效");
        }
        if (!"admin".equalsIgnoreCase(operator.getUsername()) && permissionIds != null && !permissionIds.isEmpty()) {
            List<String> operatorCodes = userMapper.getPermissionCodesByUserId(operator.getId());
            List<Permission> desiredPermissions = permissionMapper.selectList(new LambdaQueryWrapper<Permission>()
                    .in(Permission::getPermissionId, permissionIds));
            boolean exceedsOperator = desiredPermissions.stream()
                    .map(Permission::getPermissionCode)
                    .anyMatch(code -> !operatorCodes.contains(code));
            if (exceedsOperator) {
                throw new IllegalArgumentException("不能授予超出当前账号范围的权限");
            }
        }
        assignPermissionsInternal(roleId, permissionIds);
    }

    private void assignPermissionsInternal(Long roleId, List<Long> permissionIds) {
        requireRole(roleId);
        List<Long> normalized = permissionIds == null ? List.of() : new ArrayList<>(new LinkedHashSet<>(
                permissionIds.stream().filter(Objects::nonNull).toList()));
        if (!normalized.isEmpty()) {
            long count = permissionMapper.selectCount(new LambdaQueryWrapper<Permission>()
                    .in(Permission::getPermissionId, normalized));
            if (count != normalized.size()) {
                throw new IllegalArgumentException("选择的权限不存在或已被删除");
            }
        }
        remove(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
        for (Long permissionId : normalized) {
            RolePermission relation = new RolePermission();
            relation.setRoleId(roleId);
            relation.setPermissionId(permissionId);
            save(relation);
        }
    }

    private Role requireRole(Long roleId) {
        Role role = roleId == null ? null : roleMapper.selectById(roleId);
        if (role == null) {
            throw new IllegalArgumentException("角色不存在");
        }
        return role;
    }
}
