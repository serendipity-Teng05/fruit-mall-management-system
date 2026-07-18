package com.example.fruitmallmanagementsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.fruitmallmanagementsystem.entity.Role;
import com.example.fruitmallmanagementsystem.entity.RolePermission;
import com.example.fruitmallmanagementsystem.entity.UserRole;
import com.example.fruitmallmanagementsystem.mapper.RoleMapper;
import com.example.fruitmallmanagementsystem.mapper.RolePermissionMapper;
import com.example.fruitmallmanagementsystem.mapper.UserRoleMapper;
import com.example.fruitmallmanagementsystem.service.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;

    public RoleServiceImpl(UserRoleMapper userRoleMapper, RolePermissionMapper rolePermissionMapper) {
        this.userRoleMapper = userRoleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
    }

    @Override
    public List<Role> getAllRoles() {
        return list(new LambdaQueryWrapper<Role>().orderByAsc(Role::getRoleName));
    }

    @Override
    public Role getRoleById(Long roleId) { return getById(roleId); }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addRole(Role role) {
        validateRole(role, null);
        role.setRoleCode(normalizeCode(role.getRoleCode()));
        role.setRoleName(role.getRoleName().trim());
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        save(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(Role role) {
        Role existing = role == null || role.getRoleId() == null ? null : getById(role.getRoleId());
        if (existing == null) {
            throw new IllegalArgumentException("角色不存在");
        }
        role.setRoleCode(existing.getRoleCode());
        validateRole(role, role.getRoleId());
        existing.setRoleName(role.getRoleName().trim());
        existing.setRoleDesc(role.getRoleDesc());
        existing.setUpdateTime(LocalDateTime.now());
        updateById(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long roleId) {
        Role role = getById(roleId);
        if (role == null) {
            throw new IllegalArgumentException("角色不存在");
        }
        if (List.of("ADMIN", "CUSTOMER", "STAFF").contains(role.getRoleCode())) {
            throw new IllegalArgumentException("系统内置角色不能删除");
        }
        long userCount = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getRoleId, roleId));
        if (userCount > 0) {
            throw new IllegalArgumentException("该角色仍分配给 " + userCount + " 名人员，请先调整用户角色");
        }
        long permissionCount = rolePermissionMapper.selectCount(new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, roleId));
        if (permissionCount > 0) {
            throw new IllegalArgumentException("该角色仍关联权限，请先清空角色权限");
        }
        removeById(roleId);
    }

    private void validateRole(Role role, Long currentRoleId) {
        if (role == null || !StringUtils.hasText(role.getRoleCode())) {
            throw new IllegalArgumentException("角色编码不能为空");
        }
        if (!StringUtils.hasText(role.getRoleName())) {
            throw new IllegalArgumentException("角色名称不能为空");
        }
        String code = normalizeCode(role.getRoleCode());
        if (!code.matches("[A-Z][A-Z0-9_]{2,49}")) {
            throw new IllegalArgumentException("角色编码需为3-50位大写字母、数字或下划线");
        }
        Role sameCode = lambdaQuery().eq(Role::getRoleCode, code).one();
        if (sameCode != null && !Objects.equals(sameCode.getRoleId(), currentRoleId)) {
            throw new IllegalArgumentException("角色编码已存在");
        }
        Role sameName = lambdaQuery().eq(Role::getRoleName, role.getRoleName().trim()).one();
        if (sameName != null && !Objects.equals(sameName.getRoleId(), currentRoleId)) {
            throw new IllegalArgumentException("角色名称已存在");
        }
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
