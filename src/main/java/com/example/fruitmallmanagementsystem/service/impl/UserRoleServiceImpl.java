package com.example.fruitmallmanagementsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.fruitmallmanagementsystem.entity.Role;
import com.example.fruitmallmanagementsystem.entity.User;
import com.example.fruitmallmanagementsystem.entity.UserRole;
import com.example.fruitmallmanagementsystem.mapper.RoleMapper;
import com.example.fruitmallmanagementsystem.mapper.UserMapper;
import com.example.fruitmallmanagementsystem.mapper.UserRoleMapper;
import com.example.fruitmallmanagementsystem.service.UserRoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements UserRoleService {
    private static final String ADMIN = "ADMIN";
    private static final String CUSTOMER = "CUSTOMER";
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    public UserRoleServiceImpl(UserMapper userMapper, RoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    public UserRole getById(Long id) {
        return super.getById(id);
    }

    @Override
    public List<UserRole> getRolesByUserId(Long userId) {
        return list(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .orderByAsc(UserRole::getRoleId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addUserRole(UserRole relation) {
        if (relation == null || relation.getUserId() == null || relation.getRoleId() == null) {
            throw new IllegalArgumentException("人员和角色不能为空");
        }
        List<Long> roleIds = new ArrayList<>(getRolesByUserId(relation.getUserId())
                .stream().map(UserRole::getRoleId).toList());
        roleIds.add(relation.getRoleId());
        assignRoles(relation.getUserId(), roleIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserRole(Long id) {
        UserRole relation = id == null ? null : getById(id);
        if (relation == null) {
            throw new IllegalArgumentException("用户角色关系不存在");
        }
        List<Long> roleIds = getRolesByUserId(relation.getUserId()).stream()
                .filter(item -> !Objects.equals(item.getId(), id))
                .map(UserRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            throw new IllegalArgumentException("每名人员至少保留一个角色");
        }
        assignRoles(relation.getUserId(), roleIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        assignRolesInternal(userId, roleIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRolesByOperator(Long userId, List<Long> roleIds, User operator) {
        if (operator == null) {
            throw new IllegalArgumentException("登录状态无效");
        }
        User target = userId == null ? null : userMapper.selectById(userId);
        if (target == null) {
            throw new IllegalArgumentException("人员不存在");
        }
        List<Role> desiredRoles = loadAndValidateRoles(roleIds);
        List<String> currentCodes = roleCodesForUser(userId);
        boolean adminRoleChanged = currentCodes.contains(ADMIN) != containsCode(desiredRoles, ADMIN);
        boolean protectedTarget = "admin".equalsIgnoreCase(target.getUsername());
        boolean superOperator = "admin".equalsIgnoreCase(operator.getUsername());
        if ((adminRoleChanged || protectedTarget) && !superOperator) {
            throw new IllegalArgumentException("只有超级管理员可以调整管理员账号或 ADMIN 角色");
        }
        if (protectedTarget && !containsCode(desiredRoles, ADMIN)) {
            throw new IllegalArgumentException("超级管理员必须保留 ADMIN 角色");
        }
        if (currentCodes.contains(ADMIN) && !containsCode(desiredRoles, ADMIN)) {
            Role adminRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                    .eq(Role::getRoleCode, ADMIN).last("LIMIT 1"));
            long adminCount = adminRole == null ? 0 : count(new LambdaQueryWrapper<UserRole>()
                    .eq(UserRole::getRoleId, adminRole.getRoleId()));
            if (adminCount <= 1) {
                throw new IllegalArgumentException("系统至少需要保留一名管理员");
            }
        }
        assignRolesInternal(target.getId(), desiredRoles.stream().map(Role::getRoleId).toList());
    }

    private void assignRolesInternal(Long userId, List<Long> roleIds) {
        User user = userId == null ? null : userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("人员不存在");
        }
        if (Integer.valueOf(0).equals(user.getStatus())) {
            throw new IllegalArgumentException("停用人员不能分配角色");
        }
        List<Role> roles = loadAndValidateRoles(roleIds);
        if (containsCode(roles, CUSTOMER) && roles.size() > 1) {
            throw new IllegalArgumentException("顾客角色不能与后台管理角色同时分配");
        }
        remove(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        for (Role role : roles) {
            UserRole relation = new UserRole();
            relation.setUserId(userId);
            relation.setRoleId(role.getRoleId());
            relation.setCreateTime(LocalDateTime.now());
            save(relation);
        }
    }

    private List<Role> loadAndValidateRoles(List<Long> roleIds) {
        List<Long> normalized = roleIds == null ? List.of() : new ArrayList<>(new LinkedHashSet<>(
                roleIds.stream().filter(Objects::nonNull).toList()));
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("请至少选择一个角色");
        }
        List<Role> roles = roleMapper.selectList(new LambdaQueryWrapper<Role>().in(Role::getRoleId, normalized));
        Map<Long, Role> roleMap = roles.stream().collect(Collectors.toMap(Role::getRoleId, Function.identity()));
        if (roleMap.size() != normalized.size()) {
            throw new IllegalArgumentException("选择的角色不存在或已被删除");
        }
        return normalized.stream().map(roleMap::get).toList();
    }

    private List<String> roleCodesForUser(Long userId) {
        return userMapper.getRoleCodesByUserId(userId);
    }

    private boolean containsCode(List<Role> roles, String code) {
        return roles.stream().anyMatch(role -> code.equalsIgnoreCase(role.getRoleCode()));
    }
}
