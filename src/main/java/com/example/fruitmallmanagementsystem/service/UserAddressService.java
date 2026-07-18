package com.example.fruitmallmanagementsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.fruitmallmanagementsystem.entity.UserAddress;

import java.util.List;

public interface UserAddressService extends IService<UserAddress> {
    List<UserAddress> listForUser(Long userId);
    UserAddress getForUser(Long addressId, Long userId);
    UserAddress saveForUser(UserAddress address, Long userId);
    void deleteForUser(Long addressId, Long userId);
    void setDefault(Long addressId, Long userId);
}
