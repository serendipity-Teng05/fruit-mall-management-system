package com.example.fruitmallmanagementsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.fruitmallmanagementsystem.entity.Permission;
import com.example.fruitmallmanagementsystem.mapper.PermissionMapper;
import com.example.fruitmallmanagementsystem.service.PermissionService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {
    @Override
    public List<Permission> getAllPermissions() { return list(); }

    @Override
    public void addPermission(Permission permission) { save(permission); }

    @Override
    public void updatePermission(Permission permission) { updateById(permission); }

    @Override
    public void deletePermission(Long permissionId) { removeById(permissionId); }
}