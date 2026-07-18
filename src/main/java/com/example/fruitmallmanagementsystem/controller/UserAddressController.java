package com.example.fruitmallmanagementsystem.controller;

import com.example.fruitmallmanagementsystem.common.Result;
import com.example.fruitmallmanagementsystem.entity.User;
import com.example.fruitmallmanagementsystem.entity.UserAddress;
import com.example.fruitmallmanagementsystem.service.UserAddressService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mall/addresses")
public class UserAddressController {
    private final UserAddressService addressService;

    public UserAddressController(UserAddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public Result<List<UserAddress>> list(@RequestAttribute("currentUser") User currentUser) {
        return Result.success(addressService.listForUser(currentUser.getId()));
    }

    @PostMapping
    public Result<UserAddress> create(@RequestBody UserAddress address,
                                      @RequestAttribute("currentUser") User currentUser) {
        address.setId(null);
        return Result.success(addressService.saveForUser(address, currentUser.getId()));
    }

    @PutMapping("/{id}")
    public Result<UserAddress> update(@PathVariable Long id,
                                      @RequestBody UserAddress address,
                                      @RequestAttribute("currentUser") User currentUser) {
        address.setId(id);
        return Result.success(addressService.saveForUser(address, currentUser.getId()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestAttribute("currentUser") User currentUser) {
        addressService.deleteForUser(id, currentUser.getId());
        return Result.success();
    }

    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id,
                                   @RequestAttribute("currentUser") User currentUser) {
        addressService.setDefault(id, currentUser.getId());
        return Result.success();
    }
}
