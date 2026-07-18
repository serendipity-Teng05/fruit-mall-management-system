package com.example.fruitmallmanagementsystem.service;

import com.example.fruitmallmanagementsystem.entity.Role;
import java.util.List;

public interface RoleService {
    List<Role> getAllRoles();
    Role getRoleById(Long roleId);
    void addRole(Role role);
    void updateRole(Role role);
    void deleteRole(Long roleId);
}