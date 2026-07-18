package com.example.fruitmallmanagementsystem.service;

import com.example.fruitmallmanagementsystem.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.fruitmallmanagementsystem.dto.UserSaveDTO;
import com.example.fruitmallmanagementsystem.dto.CustomerRegisterDTO;

import java.util.List;

// 关键点：必须继承 IService<User>，这样就自动拥有了增删改查方法
public interface UserService extends IService<User> {
    User getByUsername(String username);
    List<String> getPermissionCodesByUserId(Long userId);
    List<String> getRoleCodesByUserId(Long userId);
    Page<User> pageWithRoles(Integer pageNum, Integer pageSize, String keyword, Boolean includeDisabled);
    User saveManagedUser(UserSaveDTO dto, User operator);
    User registerCustomer(CustomerRegisterDTO dto);
    void changeStatus(Long userId, Integer status, Long operatorId);
    void attachRoles(List<User> users);
}
