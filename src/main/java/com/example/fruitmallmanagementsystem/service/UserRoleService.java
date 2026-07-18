package com.example.fruitmallmanagementsystem.service;

import com.example.fruitmallmanagementsystem.entity.UserRole;
import com.example.fruitmallmanagementsystem.entity.User;
import java.util.List;

public interface UserRoleService {
    UserRole getById(Long id);

    List<UserRole> getRolesByUserId(Long userId);

    void addUserRole(UserRole userRole);

    void deleteUserRole(Long id);

    void assignRoles(Long userId, List<Long> roleIds);

    void assignRolesByOperator(Long userId, List<Long> roleIds, User operator);
}
