package com.example.fruitmallmanagementsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.fruitmallmanagementsystem.entity.UserAddress;
import com.example.fruitmallmanagementsystem.mapper.UserAddressMapper;
import com.example.fruitmallmanagementsystem.service.UserAddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress>
        implements UserAddressService {

    @Override
    public List<UserAddress> listForUser(Long userId) {
        return list(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getUpdateTime)
                .orderByDesc(UserAddress::getId));
    }

    @Override
    public UserAddress getForUser(Long addressId, Long userId) {
        if (addressId == null || userId == null) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getId, addressId)
                .eq(UserAddress::getUserId, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAddress saveForUser(UserAddress input, Long userId) {
        validate(input, userId);
        if (input.getId() == null && count(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)) >= 20) {
            throw new IllegalStateException("每个账号最多保存20个收货地址");
        }
        input.setRecipientName(input.getRecipientName().trim());
        input.setPhone(input.getPhone().trim());
        input.setProvince(input.getProvince().trim());
        input.setCity(input.getCity().trim());
        input.setDistrict(trim(input.getDistrict()));
        input.setDetailAddress(input.getDetailAddress().trim());
        LocalDateTime now = LocalDateTime.now();
        UserAddress address;
        if (input.getId() == null) {
            address = input;
            address.setUserId(userId);
            address.setCreateTime(now);
            if (count(new LambdaQueryWrapper<UserAddress>().eq(UserAddress::getUserId, userId)) == 0) {
                address.setIsDefault(1);
            }
        } else {
            address = getForUser(input.getId(), userId);
            if (address == null) {
                throw new IllegalArgumentException("收货地址不存在");
            }
            address.setRecipientName(input.getRecipientName().trim());
            address.setPhone(input.getPhone().trim());
            address.setProvince(input.getProvince().trim());
            address.setCity(input.getCity().trim());
            address.setDistrict(trim(input.getDistrict()));
            address.setDetailAddress(input.getDetailAddress().trim());
            if (input.getIsDefault() != null) {
                address.setIsDefault(input.getIsDefault());
            }
        }
        address.setIsDefault(Integer.valueOf(1).equals(address.getIsDefault()) ? 1 : 0);
        address.setUpdateTime(now);
        if (address.getIsDefault() == 1) {
            clearDefault(userId, address.getId());
        }
        saveOrUpdate(address);
        return address;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteForUser(Long addressId, Long userId) {
        UserAddress address = getForUser(addressId, userId);
        if (address == null) {
            throw new IllegalArgumentException("收货地址不存在");
        }
        removeById(addressId);
        if (Integer.valueOf(1).equals(address.getIsDefault())) {
            UserAddress next = getOne(new LambdaQueryWrapper<UserAddress>()
                    .eq(UserAddress::getUserId, userId)
                    .orderByDesc(UserAddress::getUpdateTime)
                    .last("LIMIT 1"));
            if (next != null) {
                setDefault(next.getId(), userId);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long addressId, Long userId) {
        UserAddress address = getForUser(addressId, userId);
        if (address == null) {
            throw new IllegalArgumentException("收货地址不存在");
        }
        clearDefault(userId, addressId);
        update(new LambdaUpdateWrapper<UserAddress>()
                .eq(UserAddress::getId, addressId)
                .eq(UserAddress::getUserId, userId)
                .set(UserAddress::getIsDefault, 1)
                .set(UserAddress::getUpdateTime, LocalDateTime.now()));
    }

    private void clearDefault(Long userId, Long keepId) {
        LambdaUpdateWrapper<UserAddress> update = new LambdaUpdateWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getIsDefault, 1)
                .set(UserAddress::getIsDefault, 0);
        if (keepId != null) {
            update.ne(UserAddress::getId, keepId);
        }
        update(update);
    }

    private void validate(UserAddress address, Long userId) {
        if (address == null || userId == null) {
            throw new IllegalArgumentException("收货地址参数不完整");
        }
        if (!StringUtils.hasText(address.getRecipientName())
                || !StringUtils.hasText(address.getPhone())
                || !StringUtils.hasText(address.getProvince())
                || !StringUtils.hasText(address.getCity())
                || !StringUtils.hasText(address.getDetailAddress())) {
            throw new IllegalArgumentException("收货人、电话、省市和详细地址不能为空");
        }
        if (!address.getPhone().trim().matches("1[3-9]\\d{9}")) {
            throw new IllegalArgumentException("请输入正确的手机号");
        }
        if (address.getRecipientName().trim().length() > 50
                || address.getProvince().trim().length() > 50
                || address.getCity().trim().length() > 50
                || (StringUtils.hasText(address.getDistrict()) && address.getDistrict().trim().length() > 50)
                || address.getDetailAddress().trim().length() > 200) {
            throw new IllegalArgumentException("收货地址字段长度超出限制");
        }
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
